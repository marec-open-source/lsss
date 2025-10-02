package no.imr.lsss.framework.wizards.newsurvey;

import no.imr.lsss.framework.config.survey.survey.SurveyConf;

final class SurveyWizardStep extends ConfigurationUnitWizardStep {
   private final SurveyConf surveyConf;

   SurveyWizardStep(SurveyConf surveyConf) {
      super(surveyConf);

      this.surveyConf = surveyConf;
   }

   @Override
   public boolean canProceed() {
      return surveyConf.getSurvey() != null;
   }
}
