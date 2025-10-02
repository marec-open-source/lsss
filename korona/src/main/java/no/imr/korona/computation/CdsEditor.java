package no.imr.korona.computation;

import no.imr.korona.Korona;
import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Editor for {@link ModuleContainer}.
 */
public final class CdsEditor extends ConfigFileParameterEditor {
   private final Korona korona;
   private final FileParameter cdsFileParameter;

   public CdsEditor(ConfigFileService configFileService, ConfigFileSettings configFileSettings) {
      this(configFileService, configFileSettings, configFileSettings.getModuleConfigurationFileParameter());
   }

   public CdsEditor(ConfigFileService configFileService, ConfigFileSettings configFileSettings, FileParameter cdsFileParameter) {
      super(configFileService, configFileSettings);

      this.cdsFileParameter = cdsFileParameter;
      korona = new Korona();
   }

   @Override
   public boolean createNew(@Nullable Component referenceComponent) {
      return edit(referenceComponent, true);
   }

   @Override
   public boolean edit(@Nullable Component referenceComponent, boolean editable) {
      ModuleContainer moduleContainer = new WorkerDialog(referenceComponent, "Loading module configuration...")
            .setWaitUntilFinishedIfCancelled(false)
            .startMakeValue(asyncHandle -> loadModuleContainer(cdsFileParameter.getFile()));
      if (moduleContainer == null) {
         return false;
      }
      return showModuleEditorAndSave(moduleContainer, referenceComponent, editable);
   }

   private ModuleContainer loadModuleContainer(@Nullable Path cdsFile) throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(korona, getConfigFileSettings());
      if (cdsFile != null && Files.exists(cdsFile)) {
         moduleContainer.readConfiguration(cdsFile);
      }
      return moduleContainer;
   }

   private boolean showModuleEditorAndSave(ModuleContainer moduleContainer, @Nullable Component referenceComponent, boolean editable) {
      boolean ok = new ModuleEditor(moduleContainer, editable, referenceComponent)
            .setModulePredicate(getConfigFileSettings().getModulePredicate())
            .show()
            .getOK();
      if (ok) {
         save(moduleContainer, referenceComponent);
      }
      return ok;
   }

   private boolean save(ModuleContainer moduleContainer, @Nullable Component referenceComponent) {
      Path file = cdsFileParameter.getFile();
      if (file == null) {
         JFileChooser fileChooser = new JFileChooser(FileUtils.toFile(cdsFileParameter.getDefaultBrowseDirectory()));
         fileChooser.setDialogTitle("Select module configuration file");
         fileChooser.setApproveButtonText("Select module configuration file");
         fileChooser.addChoosableFileFilter(new SuffixFileFilter(KoronaUtils.CDS_FILE_TYPE));
         int returnVal = fileChooser.showSaveDialog(referenceComponent);

         if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = KoronaUtils.CDS_FILE_TYPE.ensureSuffix(fileChooser.getSelectedFile().toPath());
            cdsFileParameter.setFile(file);
         } else {
            return false;
         }
      }
      try {
         moduleContainer.writeConfiguration(file);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error saving " + file, e);
      }
      return true;
   }
}
