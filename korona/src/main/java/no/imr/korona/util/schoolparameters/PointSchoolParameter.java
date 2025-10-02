package no.imr.korona.util.schoolparameters;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import org.dom4j.Element;

import java.awt.geom.Point2D;
import java.util.List;

public abstract class PointSchoolParameter<T extends Point2D> extends BaseFloatSchoolParameter {
   private T value = createValue(0, 0);

   PointSchoolParameter(Name name, String unit, String format) {
      super(name, unit, format);
   }

   public T getValue() {
      return value;
   }

   public void setValue(T value) {
      this.value = value;
   }

   @Override
   public String getDisplayValue() {
      return format(value.getX()) + ", " + format(value.getY());
   }

   @Override
   public Element toXml() {
      return createElement()
            .addAttribute("x", Utils.toString(value.getX()))
            .addAttribute("y", Utils.toString(value.getY()));
   }

   @Override
   public void doRestoreFromXml(Element element) {
      double x = Double.parseDouble(element.attributeValue("x"));
      double y = Double.parseDouble(element.attributeValue("y"));

      value = createValue(x, y);
   }

   @Override
   public List<String> getExportNames() {
      String name = getName().persistentName();
      return List.of(
            name + ".x",
            name + ".y");
   }

   @Override
   public List<String> getExportValues() {
      return List.of(
            format(value.getX()),
            format(value.getY()));
   }

   abstract T createValue(double x, double y);
}
