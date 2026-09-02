package no.imr.korona.computation.broadband.notchfilter;

import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterTableGUI;
import no.imr.tools.parameter.gui.ParameterTableModel;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.io.IOException;

public final class BroadbandNotchFilterFileParameterEditor extends ConfigFileParameterEditor<BroadbandNotchFiltersFileService> {
   BroadbandNotchFilterFileParameterEditor(BroadbandNotchFiltersFileService configFileService, ConfigFileSettings configFileSettings) {
      super(configFileService, configFileSettings);
   }

   @Override
   public boolean edit(@Nullable Component referenceComponent, boolean editable) {
      BroadbandNotchFilterModuleConfig config;
      try {
         Element element = XmlUtils.readDocument(getFile()).getRootElement();
         config = new BroadbandNotchFilterModuleConfig(element);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error reading " + getFile(), e);
         return false;
      }
      return show(referenceComponent, config, editable);
   }

   @Override
   public boolean createNew(@Nullable Component referenceComponent) {
      BroadbandNotchFilterModuleConfig config = new BroadbandNotchFilterModuleConfig();
      config.getBroadbandTemporalNotchFilterConfigs().add(new BroadbandTemporalNotchFilterConfig());
      return show(referenceComponent, config, true);
   }

   private boolean show(@Nullable Component referenceComponent, BroadbandNotchFilterModuleConfig config, boolean editable) {
      ParameterTableModel<BroadbandTemporalNotchFilterConfig> tableModel = new ParameterTableModel<>(BroadbandTemporalNotchFilterConfig::new, config.getBroadbandTemporalNotchFilterConfigs())
            .setEditable(editable);
      ParameterTableGUI<BroadbandTemporalNotchFilterConfig> tableGUI = new ParameterTableGUI<>(tableModel);
      boolean ok = new ConfigurableGUIDialog(referenceComponent, "Broadband notch filters", config)
            .setHelpID(KoronaHelp.BROADBAND_NOTCH_FILTERS)
            .setMinimumSize(800, 0)
            .setScrollable(false)
            .setGUI(tableGUI.createScrollPane())
            .show();
      if (ok) {
         getFileParameter().saveXml(referenceComponent, config.toXml());
      }
      return ok;
   }
}
