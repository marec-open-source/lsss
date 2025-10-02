package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSchoolParameter extends Configurable {
   public static final String NOT_AVAILABLE = "N/A";

   private final String unit;
   private boolean initializedFromXml;

   protected BaseSchoolParameter(Name name, String unit) {
      super(name);

      this.unit = unit;
   }

   public abstract String getDisplayValue();

   public boolean isDisplayParameter() {
      return true;
   }

   public String getUnit() {
      return unit;
   }

   @Override
   public abstract Element toXml();

   @Override
   public void fromXml(Element element) {
      doRestoreFromXml(element);
      initializedFromXml = true;
   }

   public abstract void doRestoreFromXml(Element element);

   public boolean isInitializedFromXml() {
      return initializedFromXml;
   }

   public abstract List<String> getExportNames();

   public abstract List<String> getExportValues();

   public int getMinExportWidth() {
      return 0;
   }

   public List<Integer> getExportWidths() {
      List<Integer> widths = new ArrayList<>();
      for (String exportName : getExportNames()) {
         widths.add(Math.max(getMinExportWidth(), exportName.length()));
      }
      return widths;
   }

   @Override
   public String toString() {
      return getName().displayName() + ": " + getDisplayValue();
   }
}
