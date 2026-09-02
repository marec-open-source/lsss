package no.imr.korona.util.schoolparameters;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;
import org.dom4j.Element;

import java.util.List;

public final class FloatRangeSchoolParameter extends BaseFloatSchoolParameter {
   private static final String XML_MIN = "min";
   private static final String XML_MAX = "max";

   private FloatRange range = FloatRange.EMPTY_RANGE;

   public FloatRangeSchoolParameter(Name name, String unit, String format) {
      super(name, unit, format);
   }

   @Override
   public String getDisplayValue() {
      return "[ " + format(range.min()) + ", " + format(range.max()) + " ]";
   }

   @Override
   public Element toXml() {
      Element element = createElement();
      if (!range.isEmpty()) {
         element
               .addAttribute(XML_MIN, Utils.toString(range.min()))
               .addAttribute(XML_MAX, Utils.toString(range.max()));
      }
      return element;
   }

   @Override
   public void doRestoreFromXml(Element element) {
      String minAttribute = element.attributeValue(XML_MIN);
      String maxAttribute = element.attributeValue(XML_MAX);
      if (minAttribute != null && maxAttribute != null) {
         float min = Float.parseFloat(minAttribute);
         float max = Float.parseFloat(maxAttribute);
         range = FloatRange.of(min, max);
      } else {
         range = FloatRange.EMPTY_RANGE;
      }
   }

   @Override
   public List<String> getExportNames() {
      return List.of(
            XML_MIN,
            XML_MAX
      );
   }

   @Override
   public List<String> getExportValues() {
      return List.of(
            Utils.toString(range.min()),
            Utils.toString(range.max())
      );
   }

   public FloatRange getRange() {
      return range;
   }

   public void setRange(FloatRange range) {
      this.range = range;
   }
}
