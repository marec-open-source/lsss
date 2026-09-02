package no.imr.korona.util.schoolparameters;

import no.imr.tools.Utils;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.parameter.Name;
import org.dom4j.Element;

import java.util.List;

public class VecSchoolParameter extends BaseFloatSchoolParameter {
   private Vec3 value = Vec3.ZERO;

   public VecSchoolParameter(Name name, String unit, String format) {
      super(name, unit, format);
   }

   @Override
   public String getDisplayValue() {
      return format(value.x()) + ", " + format(value.y()) + ", " + format(value.z());
   }

   @Override
   public Element toXml() {
      return createElement()
            .addAttribute("x", Utils.toString(value.x()))
            .addAttribute("y", Utils.toString(value.y()))
            .addAttribute("z", Utils.toString(value.z()));
   }

   @Override
   public void doRestoreFromXml(Element element) {
      float x = Float.parseFloat(element.attributeValue("x"));
      float y = Float.parseFloat(element.attributeValue("y"));
      float z = Float.parseFloat(element.attributeValue("z"));
      value = new Vec3(x, y, z);
   }

   @Override
   public List<String> getExportNames() {
      String name = getName().persistentName();
      return List.of(
            name + ".x",
            name + ".y",
            name + ".z"
      );
   }

   @Override
   public List<String> getExportValues() {
      return List.of(
            format(value.x()),
            format(value.y()),
            format(value.z())
      );
   }

   public Vec3 getValue() {
      return value;
   }

   public void setValue(Vec3 value) {
      this.value = value;
   }
}
