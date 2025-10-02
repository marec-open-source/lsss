package no.imr.lsss.framework.config.survey.data.extra;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.swing.ViewHolder;
import org.dom4j.Element;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

public final class ExtraDataConf extends ConfigurationUnit {
   private static final String XML_EXTRA_DATA_DIR = "ExtraDataDir";

   private final ViewHolder<ExtraDataConfView> viewHolder = new ViewHolder<>(() -> new ExtraDataConfView(this));
   private final List<ExtraDataDir> extraDataDirs = new ArrayList<>();

   public ExtraDataConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("ExtraDataConf", "Extra directories"),
            "Extra directories used when backing up survey data");
   }

   @Override
   public JComponent getComponent() {
      normalize();
      return viewHolder.getComponent();
   }

   @Override
   public void removeView() {
      viewHolder.removeView();
   }

   @Override
   public boolean apply() {
      normalize();
      return true;
   }

   @Override
   public void prepareForSaveDefault() {
      extraDataDirs.clear();
   }

   public List<ExtraDataDir> getExtraDataDirs() {
      return extraDataDirs;
   }

   boolean normalize() {
      boolean changed = false;
      // Remove empty entries before the last one:
      for (int i = extraDataDirs.size() - 2; i >= 0; i--) {
         if (extraDataDirs.get(i).isBlank()) {
            extraDataDirs.remove(i);
            changed = true;
         }
      }
      // Make sure the last entry is blank:
      if (extraDataDirs.isEmpty() || !extraDataDirs.getLast().isBlank()) {
         extraDataDirs.add(new ExtraDataDir(getLSSS()));
         changed = true;
      }
      return changed;
   }

   @Override
   public void addToConfigurationXml(Element configurationElement) {
      for (ExtraDataDir extraDataDir : extraDataDirs) {
         if (extraDataDir.isBlank()) {
            continue;
         }
         Element element = new ParameterCollection(extraDataDir).toXml();
         element.setName(XML_EXTRA_DATA_DIR);
         configurationElement.add(element);
      }
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      extraDataDirs.clear();
      for (Element parametersElement : configurationElement.elements(XML_EXTRA_DATA_DIR)) {
         ExtraDataDir extraDataDir = new ExtraDataDir(getLSSS());
         new ParameterCollection(extraDataDir).fromXml(parametersElement);
         extraDataDirs.add(extraDataDir);
      }
   }
}
