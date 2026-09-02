package no.imr.korona.computation.broadband.splitting;

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
import java.nio.file.Path;

public final class BroadbandSplitterFileParameterEditor extends ConfigFileParameterEditor<BroadbandSplitterBandsFileService> {
   BroadbandSplitterFileParameterEditor(BroadbandSplitterBandsFileService configFileService, ConfigFileSettings configFileSettings) {
      super(configFileService, configFileSettings);
   }

   @Override
   public boolean edit(@Nullable Component referenceComponent, boolean editable) {
      BroadbandSplitterConfig config;
      try {
         config = new BroadbandSplitterConfig(toXml(getFile()));
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error reading " + getFile(), e);
         return false;
      }
      return show(referenceComponent, config, editable);
   }

   @Override
   public boolean createNew(@Nullable Component referenceComponent) {
      BroadbandSplitterConfig config = new BroadbandSplitterConfig();
      config.getBands().add(new BroadbandSplitterBand());
      return show(referenceComponent, config, true);
   }

   private boolean show(@Nullable Component referenceComponent, BroadbandSplitterConfig config, boolean editable) {
      ParameterTableModel<BroadbandSplitterBand> tableModel = new ParameterTableModel<>(BroadbandSplitterBand::new, config.getBands())
            .setEditable(editable);
      ParameterTableGUI<BroadbandSplitterBand> tableGUI = new ParameterTableGUI<>(tableModel);
      boolean ok = new ConfigurableGUIDialog(referenceComponent, "Broadband splitter bands", config)
            .setHelpID(KoronaHelp.BROADBAND_SPLITTER_BANDS)
            .setScrollable(false)
            .setGUI(tableGUI.createScrollPane())
            .show();
      if (ok) {
         getFileParameter().saveXml(referenceComponent, config.toXml());
      }
      return ok;
   }

   public static Element toXml(Path file) throws IOException {
      return XmlUtils.readDocument(file).getRootElement();
   }
}
