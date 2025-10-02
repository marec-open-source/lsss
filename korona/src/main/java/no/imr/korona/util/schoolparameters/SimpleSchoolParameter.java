package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;
import org.dom4j.Element;

import java.util.List;

public abstract class SimpleSchoolParameter extends BaseSchoolParameter {
   protected SimpleSchoolParameter(Name name, String unit) {
      super(name, unit);
   }

   @Override
   public String getDisplayValue() {
      return getExportValue();
   }

   protected String getExportValue() {
      return getXmlValue();
   }

   protected abstract String getXmlValue();

   protected abstract void setXmlValue(String xmlValue);

   protected String getExportName() {
      return getName().persistentName();
   }

   @Override
   public List<String> getExportNames() {
      return List.of(getExportName());
   }

   @Override
   public List<String> getExportValues() {
      return List.of(getExportValue());
   }

   @Override
   public Element toXml() {
      return createElement()
            .addText(getXmlValue());
   }

   @Override
   public void doRestoreFromXml(Element element) {
      setXmlValue(element.getText());
   }
}
