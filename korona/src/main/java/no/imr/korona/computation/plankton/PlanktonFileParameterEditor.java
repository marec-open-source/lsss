package no.imr.korona.computation.plankton;

import no.imr.korona.computation.plankton.editor.PlanktonGUI;
import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import org.jspecify.annotations.Nullable;

import java.awt.Component;

/**
 * File parameter editor for a {@link PlanktonFile}.
 */
final class PlanktonFileParameterEditor extends ConfigFileParameterEditor {
   PlanktonFileParameterEditor(ConfigFileService configFileService, ConfigFileSettings configFileSettings) {
      super(configFileService, configFileSettings);
   }

   @Override
   public boolean edit(@Nullable Component referenceComponent, boolean editable) {
      return PlanktonGUI.showDialog(referenceComponent, getFile(), editable);
   }

   @Override
   public boolean createNew(@Nullable Component referenceComponent) {
      PlanktonFile planktonFile = new PlanktonFile();
      PlanktonGUI gui = new PlanktonGUI(referenceComponent, planktonFile, true);
      if (gui.isOK()) {
         getFileParameter().saveXml(referenceComponent, planktonFile.toXml());
      }
      return gui.isOK();
   }
}
