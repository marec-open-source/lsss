package no.imr.lsss.framework.config.survey.data;

import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;
import java.util.List;

/**
 * Base class for configuration units containing survey directories.
 */
public abstract class SurveyDirectoryConf extends ConfigurationUnit {
   protected SurveyDirectoryConf(FeaturePlugin plugin, Name name, String description) {
      super(plugin, name, description);
   }

   public abstract List<SurveyDirectoryParameter> getAllDirectoryParameters();

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      return UserProfile.SURVEY_SETUP;
   }

   @Override
   public void setFromSurvey(Path surveyDir) {
      getAllDirectoryParameters().forEach(parameter -> parameter.setFromSurvey(surveyDir));
   }

   @Override
   public void prepareForSaveDefault() {
      getAllDirectoryParameters().forEach(parameter -> parameter.setFile(null));
   }
}
