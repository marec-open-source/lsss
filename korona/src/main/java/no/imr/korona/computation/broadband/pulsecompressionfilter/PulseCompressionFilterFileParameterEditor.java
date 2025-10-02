package no.imr.korona.computation.broadband.pulsecompressionfilter;

import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;

public final class PulseCompressionFilterFileParameterEditor extends ConfigFileParameterEditor {
   PulseCompressionFilterFileParameterEditor(ConfigFileService configFileService, ConfigFileSettings configFileSettings) {
      super(configFileService, configFileSettings);
   }

   @Override
   public boolean edit(@Nullable Component referenceComponent, boolean editable) {
      GuiUtils.desktopEdit(getFile(), referenceComponent);
      return true;
   }

   @Override
   public boolean createNew(@Nullable Component referenceComponent) {
      Path file = getFileParameter().getFile();
      if (file == null) {
         JFileChooser fileChooser = getFileParameter().createFileChooser();
         int returnState = fileChooser.showSaveDialog(referenceComponent);
         if (returnState == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile().toPath();
         } else {
            return false;
         }
      }

      Path installationLocation = getConfigFileService().getInstallationLocation();
      assert installationLocation != null;
      try {
         FileUtils.copy(installationLocation, file);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error copying default file\n" + installationLocation + "\nto\n" + file, e);
         return false;
      }

      getFileParameter().setFile(file);
      return edit(referenceComponent, true);
   }
}
