package no.marec.lsss.api.util.parameters;

import no.marec.lsss.api.internal.InternalLsssUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Utility class for creating instances of {@link ValueConstraint}.
 */
public final class ValueConstraints {
   private ValueConstraints() {
   }

   public static <T> ValueConstraint<T> none() {
      return _ -> null;
   }

   public static ValueConstraint<String> maxLength(int maxLength) {
      if (maxLength < 0) {
         throw new IllegalArgumentException("Max length = " + maxLength);
      }
      return of(value -> value.length() > maxLength ? "Length " + value.length() + " > " + maxLength : null,
            _ -> "Maximum " + maxLength + " characters");
   }

   public static <T extends Comparable<? super T>> ValueConstraint<T> gt(T min) {
      return of(value -> value.compareTo(min) > 0 ? null : "Too small",
            stringifier -> "> " + stringifier.stringify(min));
   }

   public static <T extends Comparable<? super T>> ValueConstraint<T> gte(T min) {
      return of(value -> value.compareTo(min) >= 0 ? null : "Too small",
            stringifier -> "≥ " + stringifier.stringify(min));
   }

   public static <T extends Comparable<? super T>> ValueConstraint<T> lt(T max) {
      return of(value -> value.compareTo(max) < 0 ? null : "Too big",
            stringifier -> "< " + stringifier.stringify(max));
   }

   public static <T extends Comparable<? super T>> ValueConstraint<T> lte(T max) {
      return of(value -> value.compareTo(max) <= 0 ? null : "Too big",
            stringifier -> "≤ " + stringifier.stringify(max));
   }

   public static <T extends Comparable<? super T>> ValueConstraint<T> gtLt(T min, T max) {
      return of(value -> value.compareTo(min) > 0 ? value.compareTo(max) < 0 ? null : "Too big" : "Too small",
            stringifier -> "(" + stringifier.stringify(min) + ", " + stringifier.stringify(max) + ")");
   }

   public static <T extends Comparable<? super T>> ValueConstraint<T> gtLte(T min, T max) {
      return of(value -> value.compareTo(min) > 0 ? value.compareTo(max) <= 0 ? null : "Too big" : "Too small",
            stringifier -> "(" + stringifier.stringify(min) + ", " + stringifier.stringify(max) + "]");
   }

   public static <T extends Comparable<? super T>> ValueConstraint<T> gteLt(T min, T max) {
      return of(value -> value.compareTo(min) >= 0 ? value.compareTo(max) < 0 ? null : "Too big" : "Too small",
            stringifier -> "[" + stringifier.stringify(min) + ", " + stringifier.stringify(max) + ")");
   }

   public static <T extends Comparable<? super T>> ValueConstraint<T> gteLte(T min, T max) {
      return of(value -> value.compareTo(min) >= 0 ? value.compareTo(max) <= 0 ? null : "Too big" : "Too small",
            stringifier -> "[" + stringifier.stringify(min) + ", " + stringifier.stringify(max) + "]");
   }

   public static <T> ValueConstraint<T> of(Function<T, @Nullable String> validator, Function<ValueStringifier<T>, @Nullable String> allowedValuesDescription) {
      return new ValueConstraint<>() {
         @Override
         public @Nullable String validate(T value) {
            return validator.apply(value);
         }

         @Override
         public @Nullable String getAllowedValuesDescription(ValueStringifier<T> stringifier) {
            return allowedValuesDescription.apply(stringifier);
         }
      };
   }

   public static <T> ValueConstraint<List<T>> listItem(ValueConstraint<T> itemConstraint, ValueStringifier<T> itemStringifier) {
      return new ValueConstraint<>() {
         @Override
         public @Nullable String validate(List<T> value) {
            for (int i = 0; i < value.size(); i++) {
               T item = value.get(i);
               String error = itemConstraint.validate(item);
               if (error != null) {
                  return "Invalid item " + (i + 1) + ": " + itemStringifier.stringify(item) + ": " + error;
               }
            }
            return null;
         }

         @Override
         public @Nullable String getAllowedValuesDescription(ValueStringifier<List<T>> stringifier) {
            return itemConstraint.getAllowedValuesDescription(itemStringifier);
         }
      };
   }

   public static <T> ValueConstraint<T> ofValues(List<T> values) {
      return new ValueConstraint<>() {
         @Override
         public @Nullable String validate(T value) {
            return values.contains(value) ? null : "Not an allowed value";
         }

         @Override
         public String getAllowedValuesDescription(ValueStringifier<T> stringifier) {
            StringBuilder sb = new StringBuilder("<code>{ ");
            int n = sb.length();
            int lines = 0;
            for (int i = 0; i < values.size(); i++) {
               T value = values.get(i);
               String allowedStringValue = stringifier.stringify(value);
               if (i > 0) {
                  if (sb.length() - n > 50) {
                     sb.append(",<br>");
                     n = sb.length();
                     lines++;
                     if (lines == 3) {
                        // Too long, truncating...
                        sb.append("...");
                        break;
                     }
                  } else {
                     sb.append(", ");
                  }
               }
               sb.append(InternalLsssUtils.escapeHtml(allowedStringValue));
            }
            sb.append(" }</code>");
            return sb.toString();
         }

         @Override
         public List<T> getAllowedValues() {
            return values;
         }
      };
   }
}
