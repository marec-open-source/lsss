package no.imr.lsss.framework.wizards.newsurvey;

import no.imr.korona.computation.CdsEditor;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.config.application.AppPreprocessingConf;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverters;
import no.imr.tools.range.RangeMap;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.stream.Stream;

public final class NewSurveyConfigFileWrapper implements ParameterContainer {
   private final ConfigFileSettingsWizardStep configFileSettingsWizardStep;
   private final LSSS lsss;
   private final AppPreprocessingConf.ConfigFileWrapper masterConfigFileWrapper;

   public final FileParameter fileParameter;
   public final @Nullable DynamicListParameter<Path> additionalFiles;
   public final BooleanParameter copyToSurveyDir = new BooleanParameter(
         new Name("CopyToSurveyDir", "Copy to survey directory"),
         true,
         "The copied file will be used from now");
   public final @Nullable BooleanParameter usePlatformSpecificVersion;

   NewSurveyConfigFileWrapper(LSSS lsss, ConfigFileSettingsWizardStep configFileSettingsWizardStep, AppPreprocessingConf.ConfigFileWrapper masterConfigFileWrapper, ConfigFileSettings configFileSettings) {
      this.configFileSettingsWizardStep = configFileSettingsWizardStep;
      this.lsss = lsss;
      this.masterConfigFileWrapper = masterConfigFileWrapper;

      fileParameter = configFileSettings.getFileParameter(masterConfigFileWrapper.getName());
      fileParameter.setFile(masterConfigFileWrapper.file.getFile());
      ConfigFileService configFileService = masterConfigFileWrapper.getConfigFileService();
      if (masterConfigFileWrapper.additionalFiles != null) {
         additionalFiles = new DynamicListParameter<>(
               new Name("Additional" + configFileService.getName().persistentName()),
               masterConfigFileWrapper.additionalFiles.getValue(), Unit.NONE, ValueConverters.PATH) {
            @Override
            public FileParameter newOptionalParameter(int index, String persistentName) {
               FileParameter newParameter = new FileParameter(
                     new Name(persistentName, configFileService.getName().displayName()),
                     null, FileParameter.Mode.FILE) {
                  @Override
                  public @Nullable Path getDefaultBrowseDirectory() {
                     Path mainFile = fileParameter.getFile();
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
               newParameter.setReferenceDirectoryManager(configFileSettings.getReferenceDirectoryManager());
               return newParameter;
            }
         };
      } else {
         additionalFiles = null;
      }

      if (masterConfigFileWrapper.getConfigFileService().canBePlatformSpecific()) {
         Path platformSpecificFile = getConfigFileByDateAndPlatform(false);
         if (platformSpecificFile != null) {
            fileParameter.setFile(platformSpecificFile);
         }
         usePlatformSpecificVersion = new BooleanParameter(
               new Name("UsePlatformSpecificVersion", "Use platform-specific version"),
               platformSpecificFile != null,
               "<file>_<Nation>_<Platform_name[Code]>_<yyyymmdd>");
         usePlatformSpecificVersion.subscribe(this::updatePlatformSpecificFile);
      } else {
         usePlatformSpecificVersion = null;
      }
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return Stream.of(
                  fileParameter,
                  additionalFiles,
                  copyToSurveyDir,
                  usePlatformSpecificVersion
            )
            .filter(Objects::nonNull)
            .toList();
   }

   private void updatePlatformSpecificFile(boolean usePlatformSpecific) {
      if (usePlatformSpecific) {
         Path platformSpecificFile = getConfigFileByDateAndPlatform(true);
         if (platformSpecificFile == null) {
            Path file = fileParameter.getFile();
            if (file == null) {
               file = masterConfigFileWrapper.file.getFile();
            }
            if (file != null) {
               String date = String.format("%08d", getPlatform().getFirstValidDate());
               String fileName = NewSurveyUtils.getPlatformSpecificBaseName(file, getSurvey()) + "_" + date + FileUtils.getSuffix(file);
               platformSpecificFile = file.resolveSibling(fileName);
            }
         }
         fileParameter.setFile(platformSpecificFile);
      } else {
         fileParameter.setFile(masterConfigFileWrapper.file.getFile());
      }
   }

   AppPreprocessingConf.ConfigFileWrapper getMasterConfigFileWrapper() {
      return masterConfigFileWrapper;
   }

   Name getName() {
      return masterConfigFileWrapper.getName();
   }

   private Survey getSurvey() {
      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      assert survey != null;
      return survey;
   }

   private Platform getPlatform() {
      return getSurvey().getPlatform();
   }

   private @Nullable Path getConfigFileByDateAndPlatform(boolean showDialogIfError) {
      Path defaultConfigFile = fileParameter.getFile();
      if (defaultConfigFile == null) {
         return null;
      }

      Path defaultConfigDir = defaultConfigFile.getParent();
      if (defaultConfigDir == null) {
         return null;
      }

      String platformSpecificFileName = NewSurveyUtils.getPlatformSpecificBaseName(defaultConfigFile, getSurvey());
      NavigableMap<Integer, String> configFileDateMap = NewSurveyUtils.createConfigFileDateMap(platformSpecificFileName, defaultConfigDir);
      RangeMap<Integer, String> rangeMap = NewSurveyUtils.createConfigFileRangeMap(configFileDateMap);

      if (rangeMap.isEmpty()) {
         return null;
      }

      int surveyStartDate = getSurvey().getStartDate();
      int surveyEndDate = getSurvey().getStopDate();

      String startSurveyFileName = rangeMap.get(surveyStartDate);
      String stopSurveyFileName = rangeMap.get(surveyEndDate);

      if (startSurveyFileName == null || !startSurveyFileName.equals(stopSurveyFileName)) {
         if (showDialogIfError) {
            JOptionPane.showMessageDialog(configFileSettingsWizardStep.getComponent(),
                  "Survey interval " + surveyStartDate + " to " + surveyEndDate +
                        " does not match one single config file for platform " + getPlatform().findPlatformName(surveyStartDate) + ".",
                  "Error", JOptionPane.ERROR_MESSAGE);
         }
         return null;
      } else {
         return defaultConfigDir.resolve(startSurveyFileName);
      }
   }
}
