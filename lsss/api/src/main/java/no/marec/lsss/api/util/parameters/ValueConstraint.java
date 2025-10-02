package no.marec.lsss.api.util.parameters;

import no.marec.lsss.api.DoNotImplement;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * For constraining the set of allowed values.
 *
 * @param <T> the value type
 */
@DoNotImplement
@FunctionalInterface
public interface ValueConstraint<T> {
   /**
    * Checks if a value is valid.
    *
    * @param value the value to check
    * @return {@code null} if valid, an error message if invalid
    */
   @Nullable String validate(T value);

   /**
    * Tests if a value is allowed.
    *
    * @param value a value
    * @return {@code true} is the value is allowed
    */
   default boolean isValid(T value) {
      return validate(value) == null;
   }

   /**
    * {@return a description of the allowed values}
    *
    * @param stringifier for converting values to strings
    */
   default @Nullable String getAllowedValuesDescription(ValueStringifier<T> stringifier) {
      return null;
   }

   /**
    * Returns the list of allowed values, if available.
    *
    * @return the list of allowed values, or {@code null} if not available
    */
   default @Nullable List<T> getAllowedValues() {
      return null;
   }

   /**
    * Converts this {@code ValueConstraint<T>} to {@code ValueConstraint<Optional<T>>}.
    *
    * @return the new constraint for optional values
    */
   default ValueConstraint<Optional<T>> forOptionalValues() {
      return ValueConstraints.of(value -> value.map(this::validate).orElse(null),
            stringifier -> {
               String text = getAllowedValuesDescription(value -> stringifier.stringify(Optional.of(value)));
               return text != null ? "Empty or " + text : null;
            });
   }

   /**
    * Creates a new composite constraint.
    *
    * @param extraConstraint an additional constraint
    * @return the combined constraint
    */
   default ValueConstraint<T> withExtraValidation(ValueConstraint<T> extraConstraint) {
      return new ValueConstraint<>() {
         @Override
         public @Nullable String validate(T value) {
            String error = extraConstraint.validate(value);
            if (error != null) {
               return error;
            }
            return ValueConstraint.this.validate(value);
         }

         @Override
         public @Nullable String getAllowedValuesDescription(ValueStringifier<T> stringifier) {
            return ValueConstraint.this.getAllowedValuesDescription(stringifier);
         }

         @Override
         public @Nullable List<T> getAllowedValues() {
            return ValueConstraint.this.getAllowedValues();
         }
      };
   }
}
