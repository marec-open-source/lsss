package no.imr.lsss.framework.config.survey.data.remote;

import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.tools.parameter.FileParameter;

public final class RemoteDirectoryParameter extends FileParameter {
   private final SurveyDirectoryParameter surveyDirectoryParameter;

   RemoteDirectoryParameter(SurveyDirectoryParameter surveyDirectoryParameter) {
      super(surveyDirectoryParameter.getName(), null, Mode.DIRECTORY);
      this.surveyDirectoryParameter = surveyDirectoryParameter;
   }

   public SurveyDirectoryParameter getSurveyDirectoryParameter() {
      return surveyDirectoryParameter;
   }
}
