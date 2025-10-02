package no.imr.tools;

import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface ValueOrError<T> {

   static <T> ValueOrError<T> of(T value) {
      return new ValueOrError.Value<>(value);
   }

   static <T> ValueOrError<T> error(String error) {
      return new ValueOrError.Error<>(error);
   }

   default boolean isPresent() {
      return this instanceof Value<?>;
   }

   default void ifPresent(Consumer<? super T> valueConsumer) {
      if (this instanceof Value<T> v) {
         valueConsumer.accept(v.value);
      }
   }

   default void ifPresentOrElse(Consumer<? super T> valueConsumer, Consumer<String> errorConsumer) {
      switch (this) {
         case Value<T> v -> valueConsumer.accept(v.value);
         case Error<T> e -> errorConsumer.accept(e.error);
      }
   }

   default void ifError(Consumer<String> errorConsumer) {
      if (this instanceof Error<T> e) {
         errorConsumer.accept(e.error);
      }
   }

   default T orElse(T defaultValue) {
      return this instanceof Value<T> v ? v.value : defaultValue;
   }

   default <X extends Throwable> T orElseThrow(Function<String, X> exceptionGenerator) throws X {
      return switch (this) {
         case Value<T> v -> v.value;
         case Error<T> e -> throw exceptionGenerator.apply(e.error);
      };
   }

   record Value<T>(T value) implements ValueOrError<T> {
   }

   record Error<T>(String error) implements ValueOrError<T> {
   }
}
