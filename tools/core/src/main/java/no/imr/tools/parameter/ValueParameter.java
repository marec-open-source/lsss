package no.imr.tools.parameter;

import no.imr.tools.swing.svg.SvgIcon;
import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Parameters that can be represented by a string value.
 *
 * @param <T> the value type
 */
public non-sealed class ValueParameter<T> extends BaseValueParameter<T> {
   private final ValueConverter<T> converter;

   public ValueParameter(Name name, T initialValue, Unit unit, ValueConstraint<T> constraint, ValueConverter<T> converter) {
      this(name, initialValue, unit, constraint, converter, "");
   }

   public ValueParameter(Name name, T initialValue, Unit unit, ValueConstraint<T> constraint, ValueConverter<T> converter, String description) {
      super(name, initialValue, unit, description, constraint);

      this.converter = converter;
   }

   public ValueParameter(Name name, T initialValue, Unit unit, ValueConverter<T> converter) {
      this(name, initialValue, unit, converter, "");
   }

   public ValueParameter(Name name, T initialValue, Unit unit, ValueConverter<T> converter, String description) {
      super(name, initialValue, unit, description, ValueConstraints.none());

      this.converter = converter;
   }

   @Override
   public String toString() {
      return getPersistentName() + " = " + getStringValue();
   }

   public String getStringValue() {
      return converter.stringify(getValue());
   }

   /**
    * Set the parameter value as a string.
    *
    * @param stringValue the new value
    * @throws ParameterException if the value is not accepted
    */
   public void setStringValue(String stringValue) {
      T value = stringToValue(stringValue);
      setValue(value);
   }

   public T stringToValue(String stringValue) {
      try {
         return converter.parse(stringValue);
      } catch (Exception e) {
         throw new ParameterException(this, e);
      }
   }

   @Override
   public @Nullable String getAllowedValuesDescription() {
      return getConstraint().getAllowedValuesDescription(converter);
   }

   public @Nullable List<String> getAllowedStringValues() {
      List<T> allowedValues = getAllowedValues();
      if (allowedValues == null) {
         return null;
      }
      return allowedValues.stream()
            .map(converter::stringify)
            .toList();
   }

   public void setSuggestedValues(List<T> values) {
      setProperty(KEY_SUGGESTED_VALUES, values);
   }

   @SuppressWarnings("unchecked")
   public List<T> getSuggestedValues() {
      return (List<T>) getProperty(KEY_SUGGESTED_VALUES);
   }

   public String toValueString(T value) {
      return converter.stringify(value);
   }

   public String toDisplayString(T value) {
      return converter.stringify(value);
   }

   public @Nullable String toTooltip(T value) {
      return null;
   }

   public @Nullable SvgIcon toIcon(T value) {
      return null;
   }

   @Override
   public Element toXml() {
      return createElement()
            .addText(getStringValue());
   }

   @Override
   public T xmlToValue(Element element) {
      return stringToValue(element.getText());
   }
}
