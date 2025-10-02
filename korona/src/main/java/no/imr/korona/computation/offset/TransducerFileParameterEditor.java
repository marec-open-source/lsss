package no.imr.korona.computation.offset;

import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.help.HelpID;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterTableGUI;
import no.imr.tools.parameter.gui.ParameterTableModel;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.io.IOException;

/**
 * File parameter editor for all flavors,{@link TransducerParameters.ParameterType}, of {@link TransducerParameterManager}.
 */
final class TransducerFileParameterEditor extends ConfigFileParameterEditor {
   private final TransducerParameters.ParameterType parameterType;

   TransducerFileParameterEditor(ConfigFileService configFileService, ConfigFileSettings configFileSettings,
                                 TransducerParameters.ParameterType parameterType) {
      super(configFileService, configFileSettings);

      this.parameterType = parameterType;
   }

   @Override
   public boolean edit(@Nullable Component referenceComponent, boolean editable) {

      try {
         Document document = XmlUtils.readDocument(getFile());
         TransducerParameterManager transducerParameterManager = new TransducerParameterManager(parameterType, document);
         return show(referenceComponent, transducerParameterManager, editable);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error reading " + getFile(), e);
         return false;
      }
   }

   @Override
   public boolean createNew(@Nullable Component referenceComponent) {
      TransducerParameterManager transducerParameterManager = new TransducerParameterManager(parameterType);
      return show(referenceComponent, transducerParameterManager, true);
   }

   private boolean show(@Nullable Component referenceComponent, TransducerParameterManager transducerParameterManager, boolean editable) {
      ParameterTableModel<TransducerParameters> tableModel = new ParameterTableModel<>(
            () -> new TransducerParameters(transducerParameterManager.getParameterType()),
            transducerParameterManager.getTransducerParametersList())
            .setEditable(editable);
      ParameterTableGUI<TransducerParameters> tableGUI = new ParameterTableGUI<>(tableModel);
      boolean ok = new ConfigurableGUIDialog(referenceComponent, getConfigFileService().getName().displayName(), transducerParameterManager)
            .setHelpID(getHelpID())
            .setCloseOnOk(tableGUI::stopEditing)
            .setNoScrollGUI(tableGUI.createScrollPane())
            .show();
      if (ok) {
         transducerParameterManager.sortAndNotify();
         getFileParameter().saveXml(referenceComponent, transducerParameterManager.toXml());
      }
      return ok;
   }

   private HelpID getHelpID() {
      return KoronaHelp.HELP_SET.createHelpID(getConfigFileService().getName().persistentName());
   }
}
