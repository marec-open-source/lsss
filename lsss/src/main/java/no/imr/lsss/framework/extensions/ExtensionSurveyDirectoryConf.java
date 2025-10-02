package no.imr.lsss.framework.extensions;

import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.framework.config.survey.data.SurveyDirectoryConf;
import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.marec.lsss.api.config.DataConfig;

import java.util.List;

final class ExtensionSurveyDirectoryConf extends SurveyDirectoryConf {
   private final DataConfig dataConfig;

   ExtensionSurveyDirectoryConf(ExtensionFeaturePlugin plugin, DataConfig dataConfig) {
      super(plugin, new Name(dataConfig.getId(), dataConfig.getLabel()), dataConfig.getDescription());

      this.dataConfig = dataConfig;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return ExtensionUtils.getParameters(dataConfig.getParameters());
   }

   @Override
   public List<SurveyDirectoryParameter> getAllDirectoryParameters() {
      return Utils.getAllOfType(dataConfig.getParameters(), SurveyDirectoryParameter.class).toList();
   }
}
