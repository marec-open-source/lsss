package no.imr.lsss.framework.config.survey.survey;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.LsssDatabaseUtils;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.framework.config.ConfigurationUtils;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.WrappingFlowLayout;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Optional;

final class SurveyConfView {
   private final SurveyConf surveyConf;
   private final LSSS lsss;
   private final ConfigurationManager configurationManager;

   private final JPanel mainPanel = new JPanel(new BorderLayout());

   private final JButton newPlatformButton = new JButton("New...");
   private final JButton editPlatformButton = new JButton("Edit...");
   private final JButton deletePlatformButton = new JButton("Delete...");

   private final JButton newSurveyButton = new JButton("New...");
   private final JButton editSurveyButton = new JButton("Edit...");
   private final JButton deleteSurveyButton = new JButton("Delete...");

   SurveyConfView(SurveyConf aSurveyConf) {
      surveyConf = aSurveyConf;
      lsss = aSurveyConf.getLSSS();
      configurationManager = lsss.getConfigurationManager();

      WhenShowingListening.connect(mainPanel, surveyConf.getParameters(), this::updateButtonsEnabled);

      newPlatformButton.addActionListener(_ -> editPlatform(null));
      editPlatformButton.addActionListener(_ -> editPlatform(surveyConf.getPlatform()));
      deletePlatformButton.addActionListener(_ -> deletePlatform());

      newSurveyButton.addActionListener(_ -> editSurvey(null));
      editSurveyButton.addActionListener(_ -> editSurvey(surveyConf.getSurvey()));
      deleteSurveyButton.addActionListener(_ -> deleteSurvey());

      JPanel platformPanel = ConfigurationUtils.createTitledButtonPanel("Platform");
      platformPanel.add(newPlatformButton);
      platformPanel.add(editPlatformButton);
      platformPanel.add(deletePlatformButton);

      JPanel surveyPanel = ConfigurationUtils.createTitledButtonPanel("Survey");
      surveyPanel.add(newSurveyButton);
      surveyPanel.add(editSurveyButton);
      surveyPanel.add(deleteSurveyButton);

      JPanel buttonsPanel = new JPanel(new WrappingFlowLayout(FlowLayout.RIGHT));
      buttonsPanel.setBorder(BorderFactory.createEtchedBorder());
      buttonsPanel.setMinimumSize(new Dimension(10, 10));
      buttonsPanel.add(platformPanel);
      buttonsPanel.add(Box.createHorizontalStrut(5));
      buttonsPanel.add(surveyPanel);

      mainPanel.add(GuiUtils.createScrollPane(surveyConf.createParameterEditor()));
      mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
   }

   JComponent getComponent() {
      return mainPanel;
   }

   private void editPlatform(@Nullable Platform aPlatformToEdit) {
      Nation nation = surveyConf.mNation.getValue().orElseThrow();
      PlatformEditor platformEditor = new PlatformEditor(lsss, nation, aPlatformToEdit);
      Platform platform = platformEditor.getStoredPlatform();
      if (platform == null) {
         return;
      }
      surveyConf.updateAllowedPlatforms();
      surveyConf.mPlatform.setValue(Optional.of(platform));
   }

   private void deletePlatform() {
      if (surveyConf.useLocalDatabase.getBooleanValue() || configurationManager.getApplicationConfiguration().getDatabaseConf().useLocalDatabase.getBooleanValue()) {
         JOptionPane.showMessageDialog(lsss.getReferenceComponent(), "Cannot delete platform from survey local database");
         return;
      }

      Platform platformToDelete = surveyConf.getPlatform();
      if (platformToDelete == null) {
         JOptionPane.showMessageDialog(lsss.getReferenceComponent(), "No platform selected");
         return;
      }
      String platformText = surveyConf.mPlatformAndName.getStringValue();
      int answer = JOptionPane.showConfirmDialog(lsss.getReferenceComponent(),
            "Delete platform " + platformText +
                  " ?\n\nAll associated surveys will be deleted from the database.\n\n",
            "Delete platform?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
      if (answer != JOptionPane.OK_OPTION) {
         return;
      }
      surveyConf.mPlatform.setValue(Optional.empty());
      new WorkerDialog(lsss.getReferenceComponent(), "Deleting platform " + platformText)
            .startWithoutCancel(() -> {
               Log.global.info("Deleting platform " + platformText);
               LsssDatabaseUtils.deletePlatform(lsss, lsss.getDatabaseManager().getConnectionManager().getDatabaseConnection(), platformToDelete);
            });
      surveyConf.updateAllowedPlatforms();
   }

   private void editSurvey(@Nullable Survey aSurveyToEdit) {
      Platform platform = surveyConf.mPlatform.getValue().orElseThrow();
      SurveyEditor surveyEditor = new SurveyEditor(lsss, platform, aSurveyToEdit);
      Survey survey = surveyEditor.getStoredSurvey();
      if (survey == null) {
         return;
      }
      surveyConf.updateAllowedSurveys();
      // Set survey empty first to make sure listener are called.
      surveyConf.mSurvey.setValue(Optional.empty());
      surveyConf.mSurvey.setValue(Optional.of(survey));
   }

   private void deleteSurvey() {
      if (surveyConf.useLocalDatabase.getBooleanValue() || configurationManager.getApplicationConfiguration().getDatabaseConf().useLocalDatabase.getBooleanValue()) {
         JOptionPane.showMessageDialog(lsss.getReferenceComponent(), "Cannot delete survey from survey local database");
         return;
      }

      Survey surveyToDelete = surveyConf.getSurvey();
      if (surveyToDelete == null) {
         JOptionPane.showMessageDialog(lsss.getReferenceComponent(), "No survey selected");
         return;
      }
      String surveyText = surveyConf.mSurvey.getStringValue();
      int answer = JOptionPane.showConfirmDialog(lsss.getReferenceComponent(),
            "Delete survey " + surveyText +
                  " ?\n\nAll associated data will be deleted from the database.\n\n",
            "Delete survey?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
      if (answer != JOptionPane.OK_OPTION) {
         return;
      }
      surveyConf.mSurvey.setValue(Optional.empty());
      new WorkerDialog(lsss.getReferenceComponent(), "Deleting survey " + surveyText)
            .startWithoutCancel(() -> {
               Log.global.info("Deleting survey " + surveyText);
               LsssDatabaseUtils.deleteSurvey(lsss, lsss.getDatabaseManager().getConnectionManager().getDatabaseConnection(), surveyToDelete);
            });
      surveyConf.updateAllowedSurveys();
   }

   private void updateButtonsEnabled() {
      newPlatformButton.setEnabled(surveyConf.mNation.getValue().isPresent() && configurationManager.canEdit(UserProfile.ADMINISTRATOR_MODE));
      editPlatformButton.setEnabled(surveyConf.mPlatformAndName.getValue().isPresent() && configurationManager.canEdit(UserProfile.ADMINISTRATOR_MODE));
      deletePlatformButton.setEnabled(surveyConf.mPlatformAndName.getValue().isPresent() && configurationManager.canEdit(UserProfile.ADMINISTRATOR_MODE));
      newSurveyButton.setEnabled(surveyConf.mPlatformAndName.getValue().isPresent() && configurationManager.canEdit(UserProfile.SURVEY_SETUP));
      editSurveyButton.setEnabled(surveyConf.mSurvey.getValue().isPresent() && configurationManager.canEdit(UserProfile.SURVEY_SETUP));
      deleteSurveyButton.setEnabled(surveyConf.mSurvey.getValue().isPresent() && configurationManager.canEdit(UserProfile.ADMINISTRATOR_MODE));
   }
}
