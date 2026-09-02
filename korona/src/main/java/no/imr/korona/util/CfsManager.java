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
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SuffixFileFilter;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

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

   public FileParameter.Copier createCfsCopier() {
      return new CfsCopier();
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

   private final class CfsCopier implements FileParameter.Copier {
      private CfsCopier() {
      }

      @Override
      public void copy(@Nullable Component referenceComponent) {
         copyOrMove(referenceComponent, true);
      }

      @Override
      public void move(@Nullable Component referenceComponent) {
         copyOrMove(referenceComponent, false);
      }

      private void copyOrMove(@Nullable Component referenceComponent, boolean copy) {
         Path oldCfsFile = cfsFileParameter.getFile();
         ConfigFileSettings configFileSettings;
         try {
            configFileSettings = loadConfigFileSettings();
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error loading " + oldCfsFile, e);
            return;
         }

         if (copy) {
            new FileParameter.DefaultCopier(cfsFileParameter).copy(referenceComponent);
         } else {
            new FileParameter.DefaultCopier(cfsFileParameter).move(referenceComponent);
         }

         Path newCfsFile = cfsFileParameter.getFile();
         if (oldCfsFile == null || newCfsFile == null || newCfsFile.equals(oldCfsFile)) {
            return;
         }
         if (!newCfsFile.getParent().equals(oldCfsFile.getParent())) {
            // The new cfs-file is in a different directory. Must save to update file references.
            try {
               configFileSettings.save(newCfsFile);
            } catch (IOException e) {
               GuiUtils.showErrorDialog(referenceComponent, "Error writing to " + newCfsFile, e);
               return;
            }
         }

         Path oldCdsFile = configFileSettings.getModuleConfigurationFileParameter().getFile();
         if (oldCdsFile == null || !Files.exists(oldCdsFile)) {
            return;
         }
         String oldCfsName = FileUtils.baseName(oldCfsFile);
         String newCfsName = FileUtils.baseName(newCfsFile);
         int cfsPrefixLength = Utils.commonPrefixLength(oldCfsName, newCfsName);
         String oldCfsSuffix = oldCfsName.substring(cfsPrefixLength);
         String newCfsSuffix = newCfsName.substring(cfsPrefixLength);
         String oldCdsName = FileUtils.baseName(oldCdsFile);
         String newCdsName = oldCdsName.endsWith(oldCfsSuffix)
               ? oldCdsName.substring(0, oldCdsName.length() - oldCfsSuffix.length()) + newCfsSuffix
               : oldCdsName;
         Path newCdsFile = newCfsFile.resolveSibling(newCdsName + KoronaUtils.CDS_FILE_SUFFIX);

         FileParameter oldCdsParameter = new FileParameter(new Name("Source"), oldCdsFile, FileParameter.Mode.FILE_OR_DIRECTORY);
         oldCdsParameter.setEnabled(false);
         FileParameter newCdsParameter = new FileParameter(new Name("Destination"), newCdsFile, FileParameter.Mode.FILE_OR_DIRECTORY);
         var cdsParameters = List.of(
               new HeaderParameter("Also " + (copy ? "copy" : "move") + " the module configuration?"),
               oldCdsParameter,
               newCdsParameter
         );
         ParameterEditor parameterEditor = new ParameterEditor(cdsParameters);
         SwingUtilities.invokeLater(parameterEditor.getInputComponent(newCdsParameter)::requestFocusInWindow);
         boolean ok = new ConfigurableGUIDialog(referenceComponent, "Module configuration", new ParameterCollection(cdsParameters))
               .setGUI(parameterEditor.getEditorComponent())
               .setMinimumSize(800, 0)
               .show();
         if (!ok) {
            return;
         }
         newCdsFile = newCdsParameter.getFile();
         if (newCdsFile == null || newCdsFile.equals(oldCdsFile)) {
            return;
         }
         if (Files.exists(newCdsFile)) {
            int overwriteAnswer = GuiUtils.showOptionDialog(referenceComponent, "Module configuration",
                  "Overwrite existing module configuration?\n"
                        + newCdsFile,
                  new String[]{"Overwrite", "Cancel"});
            if (overwriteAnswer != 0) {
               return;
            }
         }
         try {
            if (copy) {
               FileUtils.copy(oldCdsFile, newCdsFile);
            } else {
               FileUtils.move(oldCdsFile, newCdsFile);
            }
            configFileSettings.getModuleConfigurationFileParameter().setFile(newCdsFile);
            configFileSettings.save(newCfsFile);
         } catch (IOException e) {
            GuiUtils.showErrorDialog(referenceComponent,
                  "Error " + (copy ? "copying" : "moving") + " " + oldCdsFile + " to " + newCdsFile, e);
         }
      }
   }
}
