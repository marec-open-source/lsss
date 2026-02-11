package no.imr.tools.parameter;

import no.imr.tools.Utils;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Parameter for handling objects values.
 * The string values are taken from the {@link #toString(Object)}.
 */
public class ObjectParameter<T> extends ValueParameter<T> {
   public ObjectParameter(Name name, T initialValue) {
      this(name, initialValue, "");
   }

   public ObjectParameter(Name name, T initialValue, String description) {
      this(name, initialValue, List.of(initialValue), description);
   }

   public ObjectParameter(Name name, T initialValue, T[] allowedValues) {
      this(name, initialValue, allowedValues, "");
   }

   public ObjectParameter(Name name, T initialValue, T[] allowedValues, String description) {
      this(name, initialValue, Arrays.asList(allowedValues), description);
   }

   public ObjectParameter(Name name, T initialValue, List<T> allowedValues) {
      this(name, initialValue, allowedValues, "");
   }

   public ObjectParameter(Name name, T initialValue, List<T> allowedValues, String description) {
      ObjectValueConverter<T> converter = new ObjectValueConverter<>();

      super(name, initialValue, Unit.NONE, ValueConstraints.ofValues(allowedValues), converter, description);

      converter.parameter = this;
   }

   @Override
   public List<T> getAllowedValues() {
      List<T> allowedValues = super.getAllowedValues();
      if (allowedValues == null) {
         throw new IllegalStateException();
      }
      return allowedValues;
   }

   public String toString(T value) {
      return value.toString();
   }

   public @Nullable T unknownStringToValue(String unknownString) {
      return null;
   }

   @Override
   public String toDisplayString(T value) {
      if (value instanceof ObjectParameterValue objectParameterValue) {
         return objectParameterValue.getDisplayLabel();
      }
      return toString(value);
   }

   @Override
   public @Nullable String toTooltip(T value) {
      if (value instanceof ObjectParameterValue objectParameterValue) {
         return objectParameterValue.getTooltip();
      }
      return null;
   }

   @Override
   public T stringToValue(String string) {
      for (T value : getAllowedValues()) {
         if (toString(value).equals(string)) {
            return value;
         }
      }
      T value = unknownStringToValue(string);
      if (value != null) {
         return value;
      }
      throw new ParameterException(this, string);
   }

   public void shiftValue(int shift) {
      setValue(Utils.shift(getAllowedValues(), getValue(), shift));
   }

   private static final class ObjectValueConverter<T> implements ValueConverter<T> {
      private @Nullable ObjectParameter<T> parameter;

      private ObjectValueConverter() {
      }

      @Override
      public T parse(String string) {
         return Objects.requireNonNull(parameter).stringToValue(string);
      }

      @Override
      public String stringify(T value) {
         return Objects.requireNonNull(parameter).toString(value);
      }
   }
}
