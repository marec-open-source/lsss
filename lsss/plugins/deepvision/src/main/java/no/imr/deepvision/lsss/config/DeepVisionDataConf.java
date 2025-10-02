package no.imr.deepvision.lsss.config;

import no.imr.deepvision.lsss.DeepVisionPlugin;
import no.imr.deepvision.lsss.engine.mapping.DeepVisionMappingType;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.framework.config.survey.data.SurveyDirectoryConf;
import no.imr.lsss.util.LsssUtils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.listening.Listeners;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

public final class DeepVisionDataConf extends SurveyDirectoryConf {
   public final SurveyDirectoryParameter baseDir = new SurveyDirectoryParameter(
         "Select base directory for Deep Vision files",
         DeepVisionPlugin.DEEP_VISION_DIR, getLSSS());

   public final ObjectParameter<DeepVisionMappingType> distanceMapping = new ObjectParameter<>(
         new Name("DistanceMapping", "Distance mapping"),
         DeepVisionMappingType.IDENTITY, DeepVisionMappingType.values());

   public final FloatParameter distanceBehindShip = new FloatParameter(
         new Name("DistanceBehindShip", "Distance behind ship"),
         0, Unit.METER, ValueConstraints.gte(0f),
         "Distance between Deep Vision vessel and ship");

   public DeepVisionDataConf(DeepVisionPlugin plugin) {
      super(plugin, new Name("DeepVisionDataConf", "Deep Vision data files"),
            "Configuration of Deep Vision data");

      distanceMapping.addListenerAndNotify(type -> {
         distanceBehindShip.setEnabled(type == DeepVisionMappingType.IDENTITY);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(baseDir, distanceMapping, distanceBehindShip);
   }

   @Override
   public List<SurveyDirectoryParameter> getAllDirectoryParameters() {
      return List.of(baseDir);
   }

   @Override
   public void setup() {
      super.setup();

      baseDir.subscribe(Listeners.coalescingInExecutor(Exec.CACHED_THREAD_POOL, () -> {
         if (baseDir.exists()) {
            LsssUtils.setViewModulesEnabled(getPlugin());
         }
      }));
   }
}
