package no.imr.lsss.framework.wizards.newsurvey;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.SurveyManager;
import no.imr.lsss.framework.config.application.DirectoryConf;
import no.imr.lsss.framework.config.application.SurveyDirStructure;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.wizardry.WizardStep;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

final class SurveyDirectoryWizardStep extends WizardStep {
   final FileParameter surveyDirectory = new FileParameter(
         new Name("SurveyDirectory", "Survey directory"),
         null, FileParameter.Mode.DIRECTORY);

   final FileParameter surveyFile = new FileParameter(
         new Name("SurveyFile", "Survey file"),
         null, FileParameter.Mode.FILE);

   private final JPanel panel = new JPanel(new BorderLayout());
   private final LSSS lsss;
   private final DirectoryConf directoryConf;
   private @Nullable Path previousSurveyDirectory;

   SurveyDirectoryWizardStep(LSSS lsss) {
      super("Survey directory", LsssHelp.NEW_SURVEY);

      this.lsss = lsss;
      directoryConf = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf();

      surveyDirectory.subscribe(__ -> {
         getWizard().updateNextButton();
         Path file = surveyFile.getFile();
         Path currentSurveyDir = surveyDirectory.getFile();
         if (previousSurveyDirectory != null && currentSurveyDir != null
               && file != null && file.startsWith(previousSurveyDirectory)) {
            Path relativePath = previousSurveyDirectory.relativize(file);
            surveyFile.setFile(currentSurveyDir.resolve(relativePath));
         }
         previousSurveyDirectory = currentSurveyDir;
      });
      surveyFile.subscribe(__ -> {
         getWizard().updateNextButton();
      });

      setDefaultSurveyDirectory();
      setDefaultSurveyFile();

      panel.setBorder(GuiUtils.DEFAULT_MARGIN);
      panel.add(new JLabel("<html><h1>Survey directory</h1>"
            + "<p>"
            + "The survey directory will be the parent directory for the various data directories.<br>"
            + "For example: " + lsss.getConfigurationManager().getDataConf().getRawDir().getDisplayName()
            + " will be set to &lt;Survey directory>" + File.separator + directoryConf.getSelectedSurveyDirStructure().getRelativePath(DataConfLSSS.RAW_SUB_DIR)
            + "</p><br><br>"), BorderLayout.NORTH);
      panel.add(new ParameterEditor(List.of(surveyDirectory, surveyFile)).getEditorComponent());
   }

   private void setDefaultSurveyDirectory() {
      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         return;
      }
      SurveyDirStructure surveyDirStructure = directoryConf.getSelectedSurveyDirStructure();
      surveyDirectory.setFile(directoryConf.getMainDir().resolve(surveyDirStructure.getSurveyDirName(survey)));
   }

   private void setDefaultSurveyFile() {
      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         return;
      }
      SurveyDirStructure surveyDirStructure = directoryConf.getSelectedSurveyDirStructure();
      String surveyFileName = surveyDirStructure.getSurveyDirName(survey) + SurveyManager.SURVEY_FILE_SUFFIX;

      Path advancedLsssDir = directoryConf.advanced.getBooleanValue()
            ? directoryConf.getMainDirectoryParameter(DataConfLSSS.LSSS_SUB_DIR).getFile()
            : null;
      if (advancedLsssDir != null) {
         surveyFile.setFile(advancedLsssDir.resolve(surveyFileName));
         return;
      }

      Path surveyDir = surveyDirectory.getFile();
      if (surveyDir == null) {
         surveyFile.setFile(null);
         return;
      }
      surveyFile.setFile(surveyDir
            .resolve(surveyDirStructure.getRelativePath(DataConfLSSS.LSSS_SUB_DIR))
            .resolve(surveyFileName));
   }

   void surveyChanged() {
      surveyDirectory.setFile(null);
      surveyFile.setFile(null);
   }

   @Override
   public JComponent getComponent() {
      if (surveyDirectory.getFile() == null) {
         setDefaultSurveyDirectory();
      }
      if (surveyFile.getFile() == null) {
         setDefaultSurveyFile();
      }
      return panel;
   }

   @Override
   public boolean canProceed() {
      return surveyDirectory.getFile() != null &&
            surveyFile.getFile() != null &&
            surveyFile.getFile().getParent() != null;
   }
}
