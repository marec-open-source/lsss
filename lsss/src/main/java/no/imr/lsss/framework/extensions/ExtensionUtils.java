package no.imr.lsss.framework.extensions;

import no.imr.tools.parameter.BaseParameter;
import no.marec.lsss.api.modules.LsssModule;
import no.marec.lsss.api.modules.ModuleConfig;
import no.marec.lsss.api.util.parameters.BaseConfigParameter;

import java.util.ArrayList;
import java.util.List;

final class ExtensionUtils {
   private ExtensionUtils() {
   }

   static List<? extends BaseParameter<?>> getParameters(LsssModule module) {
      ModuleConfig config = module.getConfig();
      if (config == null) {
         return List.of();
      }
      List<? extends BaseConfigParameter<?>> configParameters = config.getParameters();
      return getParameters(configParameters);
   }

   static List<BaseParameter<?>> getParameters(List<? extends BaseConfigParameter<?>> configParameters) {
      List<BaseParameter<?>> parameters = new ArrayList<>(configParameters.size());
      for (BaseConfigParameter<?> configParameter : configParameters) {
         if (configParameter instanceof BaseParameter<?> parameter) {
            parameters.add(parameter);
         }
      }
      return parameters;
   }
}
