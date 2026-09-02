package no.imr.lsss.framework.config.application;

import no.imr.korona.computation.CdsEditor;
import no.imr.korona.computation.categorization.CategorizationFileService;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.config.ConfigFileCopier;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsContext;
import no.imr.korona.config.KoronaSettingsUtils;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.util.LsssUtils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverters;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.parameter.misc.ReferenceDirectory;
import no.imr.tools.swing.GuiUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

/**
 * Configuration of {@link ConfigFileSettings}.
 */
public final class AppPreprocessingConf extends ConfigurationUnit {
   private static final String PREFERENCE_CATEGORIZATION_LAST_CHECK_TIME = "categorizationLastCheckTime";
   private static final String PREFERENCE_KORONA_CONFIG_FILE_MIGRATION_DONE = "koronaConfigFileMigrationDone";

   private final ConfigFileSettingsContext context;
   private final List<ConfigFileWrapper> configFileWrappers;
   private @Nullable Path previousKoronaConfigDir;
   private @Nullable Path lastSavedKoronaConfigDir;

   public AppPreprocessingConf(FeaturePlugin plugin, Name name, ConfigFileSettingsContext context) {
      super(plugin, name, LsssUtils.infoText("Selection of default", plugin, "preprocessing configuration files"));

      this.context = context;

      previousKoronaConfigDir = getKoronaConfigDir().getFile();
      lastSavedKoronaConfigDir = previousKoronaConfigDir;

      ConfigFileSettings configFileSettings = getLSSS().getKorona().createConfigFileSettings(context);
      configFileWrappers = configFileSettings.getFileServices().stream()
            .filter(ConfigFileService::isCopyable)
            .map(configFileService -> new ConfigFileWrapper(configFileService, configFileSettings, getKoronaConfigDir().getFile()))
            .toList();
   }

   @Override
   public void setup() {
      super.setup();

      getKoronaConfigDir().subscribe(_ -> {
         for (ConfigFileWrapper configFileWrapper : configFileWrappers) {
            configFileWrapper.koronaConfigDirUpdated(previousKoronaConfigDir, getKoronaConfigDir().getFile());
         }
         previousKoronaConfigDir = getKoronaConfigDir().getFile();
      });
   }

   private ReferenceDirectory getKoronaConfigDir() {
      return getLSSS().getKorona().getKoronaSettings().getKoronaConfigDir();
   }

   public ConfigFileSettingsContext getContext() {
      return context;
   }

   public List<ConfigFileWrapper> getConfigFileWrappers() {
      return configFileWrappers;
   }

   @Override
   public void addToConfigurationXml(Element configurationElement) {
      configurationElement.add(new ConfigFileSettingsConfigurable().toXml());
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      Element element = configurationElement.elements().getFirst();
      new ConfigFileSettingsConfigurable().fromXml(element);
   }

   @Override
   public JComponent getComponent() {
      Predicate<BaseParameter<?>> parameterEnabledDecider = _ -> getConfigurationManager().canEdit(UserProfile.ADMINISTRATOR_MODE);

      ParameterEditor koronaConfigDirEditor = new ParameterEditor(List.of(getKoronaConfigDir()), new GUIConfig()
            .setParameterEnabledDecider(parameterEnabledDecider)
      );
      JPanel koronaConfigDirPanel = new JPanel(new BorderLayout());
      koronaConfigDirPanel.add(koronaConfigDirEditor.getEditorComponent());

      List<BaseParameter<?>> parameters = new ArrayList<>();
      for (ConfigFileWrapper configFileWrapper : configFileWrappers) {
         parameters.addAll(configFileWrapper.getParameters());
      }
      ParameterEditor parameterEditor = new ParameterEditor(parameters, new GUIConfig()
            .setHorizontalFill(true)
            .setInputFieldAlignment(GUIConfig.Alignment.LEFT)
            .setParameterEnabledDecider(parameterEnabledDecider)
      );

      JTextPane info = createInfoComponent("""
            <h2>Default preprocessing config setup</h2>
            <p>
               When creating a new survey, the files below will be the default suggestions for config files.
               The files used for the current survey are found under <a href="Preprocessing">Preprocessing - Config file settings</a>.
            </p>
            <p>
               <strong>Warning:</strong>
               The default files are recommended to not contain platform names (e.g. "PG.O.Sars[4174]").
            </p>
            """);
      GuiUtils.addHrefListener(info, href -> {
         switch (href) {
            case "Preprocessing" -> {
               getConfigurationManager().getSurveyConfiguration().getAllUnitsRecursively(PreprocessingConf.class)
                     .filter(preprocessingConf -> preprocessingConf.getPlugin() == getPlugin())
                     .findFirst()
                     .ifPresent(ConfigurationUnit::showInConfigurationDialog);
            }
            default -> {
            }
         }
      });
      JButton setEmptyToDefaultButton = new JButton("Set empty to default values");
      setEmptyToDefaultButton.setEnabled(getConfigurationManager().canEdit(UserProfile.ADMINISTRATOR_MODE));
      setEmptyToDefaultButton.setToolTipText("Set default values relative to " + getKoronaConfigDir().getDisplayName());
      setEmptyToDefaultButton.addActionListener(_ -> {
         Path koronaConfigDir = getKoronaConfigDir().getFile();
         if (koronaConfigDir == null) {
            JOptionPane.showMessageDialog(setEmptyToDefaultButton,
                  "Please specify " + getKoronaConfigDir().getDisplayName() + " first");
            koronaConfigDirEditor.getInputComponent(getKoronaConfigDir()).requestFocusInWindow();
         } else {
            for (ConfigFileWrapper configFileWrapper : configFileWrappers) {
               if (configFileWrapper.file.getFile() == null) {
                  configFileWrapper.useDefault(configFileWrapper.file, koronaConfigDir);
               }
            }
         }
      });
      JButton configFilesButton = new JButton("Migrate config files...");
      configFilesButton.setEnabled(getConfigurationManager().canEdit(UserProfile.ADMINISTRATOR_MODE));
      configFilesButton.setToolTipText("Shows a dialog for moving config files to subdirectories introduced in LSSS 2.16.0");
      configFilesButton.addActionListener(_ -> {
         Path koronaConfigDir = getKoronaConfigDir().getFile();
         if (koronaConfigDir == null) {
            JOptionPane.showMessageDialog(configFilesButton,
                  "Please specify " + getKoronaConfigDir().getDisplayName() + " first");
            koronaConfigDirEditor.getInputComponent(getKoronaConfigDir()).requestFocusInWindow();
         } else {
            showMoveConfigFilesDialog(koronaConfigDir, false);
         }
      });

      Box buttonPanel = Box.createHorizontalBox();
      buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      buttonPanel.add(setEmptyToDefaultButton);
      buttonPanel.add(Box.createHorizontalStrut(5));
      buttonPanel.add(configFilesButton);
      return GuiUtils.createScrollPane(List.of(
            info,
            koronaConfigDirPanel,
            Box.createVerticalStrut(10),
            new JSeparator(),
            Box.createVerticalStrut(10),
            parameterEditor.getEditorComponent(),
            buttonPanel
      ));
   }

   @Override
   public void opened() {
      lastSavedKoronaConfigDir = getKoronaConfigDir().getFile();
   }

   @Override
   public void cancelled() {
      getKoronaConfigDir().setFile(lastSavedKoronaConfigDir);
   }

   @Override
   public boolean apply() {
      if (!Objects.equals(lastSavedKoronaConfigDir, getKoronaConfigDir().getFile())) {
         lastSavedKoronaConfigDir = getKoronaConfigDir().getFile();
         getLSSS().getKorona().getKoronaSettings().save();
         KoronaSettingsUtils.copyNewOrMissingConfigFiles(getLSSS().getKorona(), getLSSS().getReferenceComponent(), getLSSS().getInterpretationSettings().isInteractiveMode());
      }
      return true;
   }

   public void doStartupChecks() {
      LSSS lsss = getLSSS();
      KoronaSettingsUtils.copyNewOrMissingConfigFiles(lsss.getKorona(), lsss.getReferenceComponent(), lsss.getInterpretationSettings().isInteractiveMode());
      if (lsss.getInterpretationSettings().isInteractiveMode()) {
         maybeShowMoveConfigFilesDialog();
         maybeChangeCategorizationXml();
      }
   }

   private @Nullable ConfigFileWrapper getConfigFileWrapper(Class<? extends ConfigFileService> configFileServiceClass) {
      for (ConfigFileWrapper configFileWrapper : configFileWrappers) {
         if (configFileServiceClass.isInstance(configFileWrapper.getConfigFileService())) {
            return configFileWrapper;
         }
      }
      return null;
   }

   private @Nullable Path getNewestCategorizationXml() {
      Path koronaConfigDir = getKoronaConfigDir().getFile();
      if (koronaConfigDir == null) {
         return null;
      }
      try {
         Path newestCategorizationXml = null;
         for (Path file : FileUtils.listFiles(koronaConfigDir)) {
            Path categorizationXml = file.resolve(Configurator.CATEGORIZATION_FILE);
            if (!Files.isRegularFile(categorizationXml)) {
               continue;
            }
            if (newestCategorizationXml == null || FileUtils.creationTime(categorizationXml).isAfter(FileUtils.creationTime(newestCategorizationXml))) {
               newestCategorizationXml = categorizationXml;
            }
         }
         return newestCategorizationXml;
      } catch (IOException _) {
         return null;
      }
   }

   private void maybeChangeCategorizationXml() {
      Preferences preferences = getConfigurationManager().getPreferences();
      if (preferences.getLong(PREFERENCE_CATEGORIZATION_LAST_CHECK_TIME, 0) > ConfigFileCopier.getLastCopyTime()) {
         return;
      }
      preferences.putLong(PREFERENCE_CATEGORIZATION_LAST_CHECK_TIME, System.currentTimeMillis());

      Path newestCategorizationXml = getNewestCategorizationXml();
      if (newestCategorizationXml == null) {
         return;
      }
      if (newestCategorizationXml.getParent().getFileName().toString().equals(CategorizationFileService.CATEGORIZATION_BASIC)) {
         return;
      }

      ConfigFileWrapper categorizationWrapper = getConfigFileWrapper(CategorizationFileService.class);
      if (categorizationWrapper == null) {
         return;
      }
      Path currentCategorizationXml = categorizationWrapper.file.getFile();
      try {
         if (currentCategorizationXml != null && !(FileUtils.creationTime(currentCategorizationXml).isBefore(FileUtils.creationTime(newestCategorizationXml)))) {
            return;
         }
      } catch (IOException _) {
         return;
      }

      int answer = JOptionPane.showConfirmDialog(getLSSS().getFrame(),
            "Found newer categorization library.\n\nReplace\n" + currentCategorizationXml + "\nwith\n" + newestCategorizationXml + "\nas default for new surveys?",
            "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      if (answer == JOptionPane.YES_OPTION) {
         categorizationWrapper.file.setFile(newestCategorizationXml);
      }
   }

   private void maybeShowMoveConfigFilesDialog() {
      Preferences preferences = getConfigurationManager().getPreferences();
      if (preferences.getBoolean(PREFERENCE_KORONA_CONFIG_FILE_MIGRATION_DONE, false)) {
         return;
      }
      preferences.putBoolean(PREFERENCE_KORONA_CONFIG_FILE_MIGRATION_DONE, true);
      Path koronaConfigDir = getKoronaConfigDir().getFile();
      if (koronaConfigDir == null) {
         return;
      }
      showMoveConfigFilesDialog(koronaConfigDir, true);
   }

   private void showMoveConfigFilesDialog(Path koronaConfigDir, boolean isStartupCheck) {
      ConfigFileSettings configFileSettings = getLSSS().getKorona().createConfigFileSettings();
      MoveConfigFilesDialog dialog = new MoveConfigFilesDialog(getLSSS().getReferenceComponent(), configFileSettings, koronaConfigDir, isStartupCheck);
      if (dialog.isUpdateLsssConfigSelected()) {
         getConfigurationManager().getApplicationConfiguration().getAllUnitsRecursively(AppPreprocessingConf.class).forEach(appPreprocessingConf -> {
            for (ConfigFileWrapper configFileWrapper : appPreprocessingConf.configFileWrappers) {
               String subDirName = configFileWrapper.configFileService.getInstallationSubDirName();
               if (subDirName.equals(".")) {
                  continue;
               }
               Path file = configFileWrapper.file.getFile();
               if (file != null && file.getParent().equals(koronaConfigDir)) {
                  configFileWrapper.file.setFile(koronaConfigDir.resolve(subDirName).resolve(file.getFileName()));
               }
               DynamicListParameter<Path> additionalFiles = configFileWrapper.additionalFiles;
               if (additionalFiles != null) {
                  additionalFiles.setValue(additionalFiles.getValue().stream()
                        .map(additionalFile -> additionalFile.getParent().equals(koronaConfigDir)
                              ? koronaConfigDir.resolve(subDirName).resolve(additionalFile.getFileName())
                              : additionalFile
                        ).toList());
               }
            }
         });
      }
   }

   /**
    * Top level configurable for all config files.
    */
   private final class ConfigFileSettingsConfigurable extends Configurable {
      private ConfigFileSettingsConfigurable() {
         super(new Name("ConfigFileSettings"));
      }

      @Override
      public Collection<? extends Configurable> getSubConfigurables() {
         return configFileWrappers;
      }
   }

   /**
    * Settings for one config file.
    */
   public static final class ConfigFileWrapper extends Configurable implements ParameterContainer {
      private final ConfigFileService configFileService;
      public final FileParameter file;
      public final @Nullable DynamicListParameter<Path> additionalFiles;

      private ConfigFileWrapper(ConfigFileService configFileService, ConfigFileSettings configFileSettings, @Nullable Path koronaConfigDir) {
         super(configFileService.getName());

         this.configFileService = configFileService;
         file = configFileSettings.getFileParameter(configFileService.getName());
         if (configFileService.isModuleConfiguration()) {
            additionalFiles = new DynamicListParameter<>(new Name("Additional" + configFileService.getName().persistentName()),
                  List.of(), Unit.NONE, ValueConverters.PATH) {
               @Override
               public FileParameter newOptionalParameter(int index, String persistentName) {
                  FileParameter fileParameter = new FileParameter(new Name(persistentName, configFileService.getName().displayName()), null, FileParameter.Mode.FILE) {
                     @Override
                     public @Nullable Path getDefaultBrowseDirectory() {
                        Path mainFile = file.getFile();
                        return mainFile != null ? mainFile.getParent() : null;
                     }

                     @Override
                     public Editor getEditor() {
                        return new CdsEditor(configFileService, configFileSettings, this);
                     }

                     @Override
                     public Copier getCopier() {
                        return new DefaultCopier(this);
                     }
                  };
                  fileParameter.setReferenceDirectoryManager(configFileSettings.getReferenceDirectoryManager());
                  return fileParameter;
               }
            };
            useDefault(file, koronaConfigDir);
            List<Path> files = configFileService.getAdditionalInstallationLocations().stream()
                  .map(file -> FileUtils.relativePath(file, configFileService.getInstallationConfigDir()))
                  .filter(Objects::nonNull)
                  .map(relativePath -> koronaConfigDir != null ? koronaConfigDir.resolve(relativePath) : Path.of(relativePath))
                  .toList();
            additionalFiles.setValue(files);
         } else {
            additionalFiles = null;
         }
      }

      @Override
      public List<? extends BaseParameter<?>> getParameters() {
         return additionalFiles != null
               ? List.of(file, additionalFiles)
               : List.of(file);
      }

      private void useDefault(FileParameter fileParameter, @Nullable Path koronaConfigDir) {
         if (koronaConfigDir != null) {
            fileParameter.setFile(configFileService.getDefaultInConfigDirectory(koronaConfigDir));
         }
      }

      public ConfigFileService getConfigFileService() {
         return configFileService;
      }

      @Override
      public Collection<? extends Configurable> getSubConfigurables() {
         return additionalFiles == null ? List.of(file) : List.of(file, additionalFiles);
      }

      @Override
      public String toString() {
         return getName().persistentName() + "; " + file.getFile();
      }

      private void koronaConfigDirUpdated(@Nullable Path previousKoronaConfigDir, @Nullable Path koronaConfigDir) {
         koronaConfigDirUpdated(previousKoronaConfigDir, koronaConfigDir, file);
         if (additionalFiles != null) {
            List<Path> newAdditionalFiles = additionalFiles.getValue().stream()
                  .map(additionalFile -> newFileFromKoronaConfigDirUpdated(previousKoronaConfigDir, koronaConfigDir, additionalFile))
                  .toList();
            additionalFiles.setValue(newAdditionalFiles);
         }
      }

      private void koronaConfigDirUpdated(@Nullable Path previousKoronaConfigDir, @Nullable Path koronaConfigDir, FileParameter fileParameter) {
         if (fileParameter.getFile() == null) {
            useDefault(fileParameter, koronaConfigDir);
         } else {
            fileParameter.setFile(newFileFromKoronaConfigDirUpdated(previousKoronaConfigDir, koronaConfigDir, fileParameter.getFile()));
         }
      }

      private static Path newFileFromKoronaConfigDirUpdated(@Nullable Path previousKoronaConfigDir, @Nullable Path koronaConfigDir, Path file) {
         if (previousKoronaConfigDir != null && FileUtils.isInDir(file, previousKoronaConfigDir)) {
            String relPath = FileUtils.relativePath(file, previousKoronaConfigDir);
            if (koronaConfigDir != null && relPath != null) {
               return koronaConfigDir.resolve(relPath);
            }
         }
         if (koronaConfigDir != null && !file.isAbsolute()) {
            return koronaConfigDir.resolve(file);
         }
         return file;
      }
   }
}
