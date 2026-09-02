package no.imr.lsss.framework.wizards.newsurvey;

import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsContext;
import no.imr.korona.config.ConfigFileSettingsUtils;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.AppPreprocessingConf;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingSetup;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.wizardry.WizardStep;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Choose config files.
 */
final class ConfigFileSettingsWizardStep extends WizardStep {
   private final SurveyDirectoryWizardStep surveyDirectoryWizardStep;
   private final LSSS lsss;
   private final ConfigFileSettingsContext context;
   private final List<NewSurveyConfigFileWrapper> configFileWrappers = new ArrayList<>();
   private JComponent component = new JLabel();
   private final JLabel titleLabel = new JLabel();
   private final ObjectParameter<CopyAlternative> copyAlternative = new ObjectParameter<>(
         new Name("Copy", "Config files to copy to survey directory:"),
         CopyAlternative.ALL, CopyAlternative.values());

   ConfigFileSettingsWizardStep(SurveyDirectoryWizardStep surveyDirectoryWizardStep, PreprocessingConf preprocessingConf) {
      super(preprocessingConf.getContext().name().displayName() + " config files", preprocessingConf.getHelpID());

      this.surveyDirectoryWizardStep = surveyDirectoryWizardStep;
      lsss = preprocessingConf.getLSSS();
      context = preprocessingConf.getContext();

      copyAlternative.subscribe(_ -> updateVisibility());

      titleLabel.setBorder(GuiUtils.DEFAULT_MARGIN);
   }

   private void updateVisibility() {
      boolean visible = copyAlternative.getValue() == CopyAlternative.SOME;
      for (NewSurveyConfigFileWrapper configFileWrapper : configFileWrappers) {
         configFileWrapper.copyToSurveyDir.setVisible(visible);
         configFileWrapper.copyToSurveyDir.notifyListeners();
      }
   }

   void surveyChanged() {
      configFileWrappers.clear();
      if (lsss.getConfigurationManager().getSurveyConf().getSurvey() != null) {
         init();
      }
   }

   private void defineConfigFileWrappers() {
      ConfigFileSettings configFileSettings = lsss.getKorona().createConfigFileSettings(context);
      AppPreprocessingConf mainAppPreprocessingConf = lsss.getConfigurationManager().getApplicationConfiguration().getAppPreprocessingConf();

      AppPreprocessingConf appPreprocessingConf = mainAppPreprocessingConf.getAllUnitsRecursively()
            .gather(Utils.allOfType(AppPreprocessingConf.class))
            .filter(conf -> conf.getContext().equals(context))
            .findFirst()
            .orElse(null);
      if (appPreprocessingConf != null) {
         for (AppPreprocessingConf.ConfigFileWrapper masterConfigFileWrapper : appPreprocessingConf.getConfigFileWrappers()) {
            configFileWrappers.add(new NewSurveyConfigFileWrapper(lsss, this, masterConfigFileWrapper, configFileSettings));
         }
      }

      updateVisibility();
   }

   @Override
   public JComponent getComponent() {
      updateTitleLabel();
      return component;
   }

   private void init() {
      defineConfigFileWrappers();

      List<BaseParameter<?>> parameters = new ArrayList<>();
      for (NewSurveyConfigFileWrapper configFileWrapper : configFileWrappers) {
         if (!parameters.isEmpty()) {
            parameters.add(SeparatorParameter.line());
         }
         parameters.addAll(configFileWrapper.getParameters());
      }

      ParameterEditor parameterEditor = new ParameterEditor(parameters, new GUIConfig()
            .setCombineInputAndDescription(BooleanParameter.class::isInstance)
            .setHorizontalFill(true)
            .setInputFieldAlignment(GUIConfig.Alignment.LEFT)
      );

      ParameterEditor copyAlternativeParameterEditor = new ParameterEditor(List.of(copyAlternative));
      copyAlternativeParameterEditor.getEditorComponent().setBorder(GuiUtils.DEFAULT_MARGIN);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(GuiUtils.createScrollPane(parameterEditor));
      mainPanel.add(copyAlternativeParameterEditor.getEditorComponent(), BorderLayout.NORTH);

      component = new JPanel(new BorderLayout());
      component.add(mainPanel);
      component.add(titleLabel, BorderLayout.NORTH);
   }

   private void updateTitleLabel() {
      Path surveyFile = surveyDirectoryWizardStep.surveyFile.getFile();
      assert surveyFile != null;
      titleLabel.setText("<html>"
            + "<h1>KORONA config files to copy from <span style='font-size: medium'>or to use directly</span></h1>"
            + "<p>If copied, the KORONA config files are copied to " + surveyFile.getParent());
   }

   @Override
   public void apply() {
      ProgressView progressView = new ProgressView("Copying config files to survey directory", 1000)
            .mainProgressAsPercentage();
      new WorkerDialog(getWizard().getDialog(), progressView.getComponent())
            .startWithoutCancel(() -> doCopy(progressView.getMainProgressHandler(), new AsyncHandle()));
   }

   private void doCopy(ProgressHandler progressHandler, AsyncHandle asyncHandle) {
      Path surveyFile = surveyDirectoryWizardStep.surveyFile.getFile();
      assert surveyFile != null;
      Path targetDir = surveyFile.getParent();
      Path referenceFilesDir = targetDir.resolve(ConfigFileSettingsUtils.REFERENCE_FILES);
      ConfigFileSettings configFileSettings = lsss.getKorona().createConfigFileSettings(context);

      Map<Path, Path> filesToCopy = new HashMap<>();

      for (NewSurveyConfigFileWrapper configFileWrapper : configFileWrappers) {
         FileParameter fileParameter = configFileSettings.getFileParameter(configFileWrapper.getName());
         Path sourceFile = configFileWrapper.fileParameter.getFile();

         boolean doCopy;
         if (copyAlternative.getValue() == CopyAlternative.SOME) {
            doCopy = configFileWrapper.copyToSurveyDir.getBooleanValue();
         } else {
            doCopy = copyAlternative.getValue() == CopyAlternative.ALL;
         }

         ConfigFileService configFileService = configFileWrapper.getMasterConfigFileWrapper().getConfigFileService();
         Path dir = configFileService.useReferenceFilesDir() ? referenceFilesDir : targetDir;
         if (sourceFile != null && doCopy) {
            Path destinationFile = dir.resolve(sourceFile.getFileName());
            filesToCopy.put(sourceFile, destinationFile);
            filesToCopy.putAll(configFileService.getAdditionalFilesToCopy(sourceFile, destinationFile));
            fileParameter.setFile(destinationFile);
         } else {
            fileParameter.setFile(sourceFile);
         }

         if (doCopy && configFileWrapper.additionalFiles != null) {
            for (Path additionalSourceFile : configFileWrapper.additionalFiles.getValue()) {
               Path copiedFile = dir.resolve(additionalSourceFile.getFileName());
               filesToCopy.put(additionalSourceFile, copiedFile);
               filesToCopy.putAll(configFileService.getAdditionalFilesToCopy(additionalSourceFile, copiedFile));
            }
         }
      }

      FileUtils.copyAllRecursively(filesToCopy, progressHandler, asyncHandle);

      String prefix = context.equals(KoronaConfigFileService.CONTEXT) ? "" : context.name().persistentName();
      Path cfsFile = targetDir.resolve(prefix + ConfigFileSettings.DEFAULT_FILE_NAME);
      try {
         configFileSettings.save(cfsFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error saving " + cfsFile, e);
      }

      lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getAllUnitsRecursively(PreprocessingConf.class).forEach(preprocessingConf -> {
         if (preprocessingConf.getContext().equals(context)) {
            preprocessingConf.normalizeSetup();
            for (PreprocessingSetup preprocessingSetup : preprocessingConf.getPreprocessingSetups()) {
               preprocessingSetup.cfsFile.setFile(cfsFile);
               preprocessingSetup.sourceDirectory.setFile(preprocessingConf.getDataConf().getRawDir().getFile());
               if (preprocessingConf.getDataConf().getProcessedDir() != null) {
                  preprocessingSetup.destinationDirectory.setFile(preprocessingConf.getDataConf().getProcessedDir().getFile());
               }
            }
         }
      });
   }

   private enum CopyAlternative {
      ALL("All"), NONE("None"), SOME("Selected only");

      private final String text;

      CopyAlternative(String text) {
         this.text = text;
      }

      @Override
      public String toString() {
         return text;
      }
   }
}
