package no.imr.lsss.framework.config.survey;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.SubDir;

import javax.swing.JFileChooser;
import java.nio.file.Files;
import java.nio.file.Path;

public class SurveyDirectoryParameter extends SurveyFileParameter {
   private final String dialogTitle;
   private final SubDir subDir;

   public SurveyDirectoryParameter(String dialogTitle, SubDir subDir, LSSS lsss) {
      super(lsss, subDir.parameterName(), Mode.DIRECTORY);

      this.dialogTitle = dialogTitle;
      this.subDir = subDir;
   }

   public SubDir getSubDir() {
      return subDir;
   }

   public void setFromSurvey(Path surveyDir) {
      setFile(getLSSS().getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getSurveySubDir(surveyDir, subDir));
   }

   @Override
   public void customizeFileChooser(JFileChooser fileChooser) {
      fileChooser.setDialogTitle(dialogTitle);
      fileChooser.setApproveButtonText("Select directory");
   }

   @Override
   public void applyFileChooser(JFileChooser fileChooser) {
      Path file = fileChooser.getSelectedFile().toPath();
      if (Files.isRegularFile(file)) {
         file = file.getParent();
      }
      setFile(file);
   }
}
