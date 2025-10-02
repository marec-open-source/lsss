package no.imr.lsss.framework.wizards.newsurvey;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.tools.Max;
import no.imr.tools.listening.Listener;
import no.imr.tools.swing.wizardry.Wizard;
import no.imr.tools.swing.wizardry.WizardStep;
import no.marec.lsss.api.util.observing.Subscription;
import org.dom4j.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wizard for creating a new survey.
 */
public final class NewSurveyWizard {
   private final LSSS lsss;
   private final Wizard wizard;
   private final List<ConfigFileSettingsWizardStep> configFileSettingsWizardSteps = new ArrayList<>();
   private final SurveyDirectoryWizardStep surveyDirectoryWizardStep;

   public NewSurveyWizard(LSSS lsss) {
      this.lsss = lsss;

      surveyDirectoryWizardStep = new SurveyDirectoryWizardStep(lsss);
      lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getAllUnitsRecursively(PreprocessingConf.class).forEach(preprocessingConf -> {
         configFileSettingsWizardSteps.add(new ConfigFileSettingsWizardStep(surveyDirectoryWizardStep, preprocessingConf));
      });
      surveyDirectoryWizardStep.surveyDirectory.subscribe(value -> {
         value.ifPresent(dir -> {
            lsss.getConfigurationManager().getSurveyConfiguration().applyRecursively(unit -> unit.setFromSurvey(dir));
         });
      });

      wizard = new Wizard(lsss.getFrame(), "New survey", createWizardSteps());
   }

   public Wizard getWizard() {
      return wizard;
   }

   public void show() {
      Element backupConfiguration = lsss.getConfigurationManager().getLsssConfiguration().toXml();
      lsss.getConfigurationManager().getSurveyConf().mSurvey.setValue(Optional.empty());

      Listener listener = this::surveyChanged;
      List<Subscription> subscriptions = List.of(
            lsss.getConfigurationManager().getSurveyConf().mSurvey.subscribe(listener)
      );

      UserProfile userProfile = lsss.getConfigurationManager().getUserProfile();
      UserProfile minimumUserProfile = lsss.getConfigurationManager().getSurveyConf().getPlatform() == null
            ? UserProfile.ADMINISTRATOR_MODE : UserProfile.SURVEY_SETUP;
      lsss.getConfigurationManager().setUserProfile(Max.of(userProfile, minimumUserProfile));

      wizard.show(1200, 600);

      lsss.getConfigurationManager().setUserProfile(userProfile);

      subscriptions.forEach(Subscription::unsubscribe);

      if (wizard.succeeded()) {
         Path surveyFile = surveyDirectoryWizardStep.surveyFile.getFile();
         assert surveyFile != null;
         lsss.getSurveyManager().saveAs(surveyFile);
         lsss.getConfigurationManager().getSurveyConfiguration().saveDefault();
      } else {
         lsss.getConfigurationManager().getLsssConfiguration().fromXml(backupConfiguration);
      }
   }

   private List<WizardStep> createWizardSteps() {
      List<WizardStep> wizardSteps = new ArrayList<>();
      wizardSteps.add(new SurveyWizardStep(lsss.getConfigurationManager().getSurveyConf()));
      wizardSteps.add(surveyDirectoryWizardStep);
      wizardSteps.add(new OptionalWizardStep());
      addConfigurationUnitWizardSteps(wizardSteps, lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf());
      addConfigurationUnitWizardSteps(wizardSteps, lsss.getConfigurationManager().getSurveyConfiguration().getDataConf());
      addConfigurationUnitWizardSteps(wizardSteps, lsss.getConfigurationManager().getSurveyConfiguration().getGridConf());
      addConfigurationUnitWizardSteps(wizardSteps, lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf());
      wizardSteps.addAll(configFileSettingsWizardSteps);
      for (int i = 3; i < wizardSteps.size(); i++) {
         wizardSteps.get(i).setIndented(true);
      }
      return wizardSteps;
   }

   private static void addConfigurationUnitWizardSteps(List<WizardStep> wizardSteps, ConfigurationUnit configurationUnit) {
      wizardSteps.add(new ConfigurationUnitWizardStep(configurationUnit));
      for (ConfigurationUnit subUnit : configurationUnit.getSubUnits()) {
         addConfigurationUnitWizardSteps(wizardSteps, subUnit);
      }
   }

   private void surveyChanged() {
      wizard.updateNextButton();
      surveyDirectoryWizardStep.surveyChanged();
      configFileSettingsWizardSteps.forEach(ConfigFileSettingsWizardStep::surveyChanged);
   }
}
