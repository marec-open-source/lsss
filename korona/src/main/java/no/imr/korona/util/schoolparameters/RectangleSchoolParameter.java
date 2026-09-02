package no.imr.korona.util.schoolparameters;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import org.dom4j.Element;

import java.awt.geom.Rectangle2D;
import java.util.List;

public class RectangleSchoolParameter extends BaseFloatSchoolParameter {
   private Rectangle2D value = new Rectangle2D.Double();

   public RectangleSchoolParameter(Name name, String unit, String format) {
      super(name, unit, format);
   }

   public Rectangle2D getValue() {
      return value;
   }

   public void setValue(Rectangle2D value) {
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
            .addAttribute("y", Utils.toString(value.getY()))
            .addAttribute("w", Utils.toString(value.getWidth()))
            .addAttribute("h", Utils.toString(value.getHeight()));
   }

   @Override
   public void doRestoreFromXml(Element element) {
      double x = Double.parseDouble(element.attributeValue("x"));
      double y = Double.parseDouble(element.attributeValue("y"));
      double w = Double.parseDouble(element.attributeValue("w"));
      double h = Double.parseDouble(element.attributeValue("h"));

      value = new Rectangle2D.Double(x, y, w, h);
   }

   @Override
   public List<String> getExportNames() {
      String name = getName().persistentName();
      return List.of(
            name + ".x.min",
            name + ".y.min",
            name + ".x.max",
            name + ".y.max"
      );
   }

   @Override
   public List<String> getExportValues() {
      return List.of(
            format(value.getMinX()),
            format(value.getMinY()),
            format(value.getMaxX()),
            format(value.getMaxY())
      );
   }
}
