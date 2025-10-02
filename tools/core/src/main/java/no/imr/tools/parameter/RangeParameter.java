package no.imr.tools.parameter;

import no.imr.tools.Utils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

public non-sealed class RangeParameter extends BaseValueParameter<FloatRange> {
   public static final ValueConverter<Float> MIN_CONVERTER = ValueConverters.of(
         string -> string.isBlank() ? Float.NEGATIVE_INFINITY : Float.parseFloat(string),
         value -> value == Float.NEGATIVE_INFINITY ? "" : Utils.toString(value)
   );
   public static final ValueConverter<Float> MAX_CONVERTER = ValueConverters.of(
         string -> string.isBlank() ? Float.POSITIVE_INFINITY : Float.parseFloat(string),
         value -> value == Float.POSITIVE_INFINITY ? "" : Utils.toString(value)
   );

   private final ValueConstraint<Float> minMaxConstraint;

   public RangeParameter(Name name, float initialMin, float initialMax, Unit unit) {
      this(name, initialMin, initialMax, unit, "");
   }

   public RangeParameter(Name name, float initialMin, float initialMax, Unit unit, String description) {
      super(name, FloatRange.of(initialMin, initialMax), unit, description, ValueConstraints.none());

      minMaxConstraint = ValueConstraints.none();
   }

   public RangeParameter(Name name, float initialMin, float initialMax, Unit unit, ValueConstraint<Float> minMaxConstraint) {
      this(name, initialMin, initialMax, unit, minMaxConstraint, "");
   }

   public RangeParameter(Name name, float initialMin, float initialMax, Unit unit, ValueConstraint<Float> minMaxConstraint, String description) {
      super(name, FloatRange.of(initialMin, initialMax), unit, description, value -> {
         String error = minMaxConstraint.validate(value.min());
         if (error != null) {
            return error;
         }
         return minMaxConstraint.validate(value.max());
      });

      this.minMaxConstraint = minMaxConstraint;
   }

   public ValueConstraint<Float> getMinMaxConstraint() {
      return minMaxConstraint;
   }

   public void setMin(float min) {
      float max = Math.max(min, getValue().max());
      setValue(FloatRange.of(min, max));
   }

   public void setMax(float max) {
      float min = Math.min(max, getValue().min());
      setValue(FloatRange.of(min, max));
   }

   @Override
   public @Nullable String getAllowedValuesDescription() {
      return minMaxConstraint.getAllowedValuesDescription(ValueConverters.FLOAT);
   }

   @Override
   public Element toXml() {
      Element element = createElement();
      element.addElement(XML_PARAMETER).addAttribute(XML_NAME, "min").setText(MIN_CONVERTER.stringify(getValue().min()));
      element.addElement(XML_PARAMETER).addAttribute(XML_NAME, "max").setText(MAX_CONVERTER.stringify(getValue().max()));
      return element;
   }

   @Override
   public FloatRange xmlToValue(Element element) {
      Element minElement = null;
      Element maxElement = null;
      for (Element subElement : element.elements()) {
         if ("min".equals(subElement.attributeValue(XML_NAME))) {
            minElement = subElement;
         } else if ("max".equals(subElement.attributeValue(XML_NAME))) {
            maxElement = subElement;
         }
      }
      if (minElement == null || maxElement == null) {
         throw new ParameterException(this, "Invalid XML: " + XmlUtils.toCompactString(element));
      }
      try {
         float min = MIN_CONVERTER.parse(minElement.getText());
         float max = MAX_CONVERTER.parse(maxElement.getText());
         return FloatRange.of(min, max);
      } catch (Exception e) {
         throw new ParameterException(this, e);
      }
   }
}
