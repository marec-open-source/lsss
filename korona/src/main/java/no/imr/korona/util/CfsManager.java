package no.imr.korona.util;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModulePredicate;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsContext;
import no.imr.korona.config.gui.ConfigFileSettingsEditor;
import no.imr.korona.config.gui.ContextVisibility;
import no.imr.korona.data.datamanager.labelling.DataFileLabelling;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SuffixFileFilter;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Functionality for file parameters for {@link ConfigFileSettings}.
 */
public final class CfsManager {
   private final Korona korona;
   private final FileParameter cfsFileParameter;
   private final @Nullable ConfigFileSettingsContext configContext;
   private final ContextVisibility configContextVisibility;
   private ModulePredicate modulePredicate = ModulePredicate.alwaysTrue();
   private Supplier<@Nullable Path> defaultBrowseDirectorySupplier = () -> null;
   private Supplier<@Nullable DataFileLabelling> dataFileLabellingSupplier = () -> null;

   public CfsManager(Korona korona, FileParameter cfsFileParameter, @Nullable ConfigFileSettingsContext configContext, ContextVisibility configContextVisibility) {
      this.korona = korona;
      this.cfsFileParameter = cfsFileParameter;
      this.configContext = configContext;
      this.configContextVisibility = configContextVisibility;
   }

   public void setModulePredicate(ModulePredicate modulePredicate) {
      this.modulePredicate = modulePredicate;
   }

   public void setDefaultBrowseDirectorySupplier(Supplier<@Nullable Path> defaultBrowseDirectorySupplier) {
      this.defaultBrowseDirectorySupplier = defaultBrowseDirectorySupplier;
   }

   public void setDataFileLabellingSupplier(Supplier<@Nullable DataFileLabelling> dataFileLabellingSupplier) {
      this.dataFileLabellingSupplier = dataFileLabellingSupplier;
   }

   public FileParameter.Editor createCfsEditor() {
      return new CfsEditor();
   }

   /**
    * Loads config file settings.
    *
    * @return config file settings
    * @throws IOException error
    */
   public ConfigFileSettings loadConfigFileSettings() throws IOException {
      ConfigFileSettings configFileSettings = createConfigFileSettings();
      Path file = cfsFileParameter.getFile();
      if (file != null) {
         configFileSettings.load(file);
         if (configContext != null) {
            configFileSettings.setContext(configContext);
         }
      }
      return configFileSettings;
   }

   private ConfigFileSettings createConfigFileSettings() {
      ConfigFileSettings configFileSettings = korona.createConfigFileSettings();
      if (configContext != null) {
         configFileSettings.setContext(configContext);
      }
      configFileSettings.setModulePredicate(modulePredicate);
      configFileSettings.setDefaultBrowseDirectorySupplier(defaultBrowseDirectorySupplier);
      configFileSettings.setDataFileLabellingSupplier(dataFileLabellingSupplier);
      return configFileSettings;
   }

   public ModuleContainer loadModuleContainer() throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(korona, loadConfigFileSettings());
      Path cdsFile = moduleContainer.getConfigFileSettings().getModuleConfigurationFile();
      if (cdsFile != null) {
         moduleContainer.readConfiguration(cdsFile);
      }
      return moduleContainer;
   }

   private static boolean save(ConfigFileSettings configFileSettings, @Nullable Component referenceComponent, FileParameter fileParameter) throws IOException {
      Path file = fileParameter.getFile();
      if (file == null) {
         JFileChooser fileChooser = new JFileChooser(FileUtils.toFile(fileParameter.getDefaultBrowseDirectory()));
         fileChooser.setDialogTitle("Select config file settings");
         fileChooser.setApproveButtonText("Select config file settings");
         fileChooser.addChoosableFileFilter(new SuffixFileFilter(ConfigFileSettings.FILE_TYPE));
         int returnVal = fileChooser.showSaveDialog(referenceComponent);

         if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = ConfigFileSettings.FILE_TYPE.ensureSuffix(fileChooser.getSelectedFile().toPath());
            fileParameter.setFile(file);
         } else {
            return false;
         }
      }
      configFileSettings.save(file);
      return true;
   }

   /**
    * Editor for {@link ConfigFileSettings}.
    */
   private final class CfsEditor implements FileParameter.Editor {
      private CfsEditor() {
      }

      @Override
      public boolean edit(@Nullable Component referenceComponent, boolean editable) {
         ConfigFileSettings configFileSettings;
         try {
            configFileSettings = loadConfigFileSettings();
         } catch (IOException e) {
            GuiUtils.showErrorDialog(referenceComponent, "Error loading " + cfsFileParameter.getFile(), e);
            return false;
         }
         return show(referenceComponent, configFileSettings, editable);
      }

      @Override
      public boolean createNew(@Nullable Component referenceComponent) {
         return show(referenceComponent, createConfigFileSettings(), true);
      }

      private boolean show(@Nullable Component referenceComponent, ConfigFileSettings configFileSettings, boolean editable) {
         boolean ok = ConfigFileSettingsEditor.showDialog(configFileSettings, referenceComponent, editable, configContextVisibility,
               KoronaHelp.CONFIG_FILE_SETTINGS);
         if (ok) {
            try {
               save(configFileSettings, referenceComponent, cfsFileParameter);
            } catch (IOException e) {
               GuiUtils.showErrorDialog(referenceComponent, "Error saving " + cfsFileParameter.getFile(), e);
            }
         }
         return ok;
      }
   }
}
