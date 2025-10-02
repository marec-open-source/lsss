package no.imr.tools.parameter;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import no.imr.tools.Utils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ValueConverters {
   private ValueConverters() {
   }

   public static final ValueConverter<Boolean> BOOLEAN = of(Boolean::parseBoolean, Object::toString);

   public static final ValueConverter<Integer> INTEGER = of(Integer::parseInt, Object::toString);

   public static final ValueConverter<Optional<Integer>> OPTIONAL_INTEGER = optional(INTEGER);

   public static final ValueConverter<Float> FLOAT = of(Float::parseFloat, Utils::toString);

   public static final ValueConverter<Optional<Float>> OPTIONAL_FLOAT = optional(FLOAT);

   public static final ValueConverter<Double> DOUBLE = of(Double::parseDouble, Utils::toString);

   public static final ValueConverter<Long> LONG = of(Long::parseLong, Object::toString);

   public static final ValueConverter<Path> PATH = of(string -> Path.of(string).normalize(), Path::toString);

   public static final ValueConverter<Optional<Path>> OPTIONAL_PATH = optional(PATH);

   public static final ValueConverter<String> STRING = of(Function.identity(), Function.identity());

   public static final ValueConverter<Optional<String>> OPTIONAL_STRING = optional(STRING);

   public static <T> ValueConverter<T> of(Function<String, T> toValueFunction, Function<T, String> toStringFunction) {
      return new ValueConverter<>() {
         @Override
         public T parse(String string) {
            return toValueFunction.apply(string);
         }

         @Override
         public String stringify(T value) {
            return toStringFunction.apply(value);
         }
      };
   }

   public static <T> ValueConverter<Optional<T>> optional(ValueConverter<T> converter) {
      return new ValueConverter<>() {
         @Override
         public Optional<T> parse(String string) throws Exception {
            return string.isEmpty() ? Optional.empty() : Optional.of(converter.parse(string));
         }

         @Override
         public String stringify(Optional<T> value) {
            return value.isPresent() ? converter.stringify(value.get()) : "";
         }
      };
   }

   public static <T> ValueConverter<List<T>> csvList(ValueConverter<T> converter) {
      return new ValueConverter<>() {
         @Override
         public List<T> parse(String string) {
            if (string.isBlank()) {
               return ImmutableList.of();
            }
            ImmutableList.Builder<T> builder = ImmutableList.builder();
            int i = 0;
            for (String part : Splitter.on(',').trimResults().split(string)) {
               try {
                  i++;
                  T item = converter.parse(part);
                  builder.add(item);
               } catch (Exception e) {
                  throw new IllegalArgumentException("Invalid item " + i + ": " + part, e);
               }
            }
            return builder.build();
         }

         @Override
         public String stringify(List<T> value) {
            return value.stream()
                  .map(converter::stringify)
                  .collect(Collectors.joining(","));
         }
      };
   }
}
