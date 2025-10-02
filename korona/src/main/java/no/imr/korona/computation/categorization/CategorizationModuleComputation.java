package no.imr.korona.computation.categorization;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public final class CategorizationModuleComputation extends GeneralPingModuleComputation {
   private CategorizationSubModule lastSubModule;
   private final Configurator configurator;
   private final FeatureExtractSubModule featureExtractSubModule;

   CategorizationModuleComputation(CategorizationModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      Path transducerRangesFile = module.getRequiredConfigFile(TransducerRangesFileService.NAME);
      TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(transducerRangesFile));

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      configurator = new Configurator(computationContext.getModuleContainer().getConfigFileSettings(), pingConfiguration.getRawFileConfiguration());

      if (configurator.getReferenceChannel() <= 0) {
         throw new ModuleConfigurationException(module, "Did not find reference frequency " + configurator.getReferenceFrequency());
      }

      module.updateSettings(configurator);

      lastSubModule = new FirstSubModule();
      Category bottomCategory = configurator.getSpecialCategory(Configurator.BOTTOM_CATEGORY_NAME);
      if (bottomCategory.isActive()) {
         lastSubModule = new BottomCategorization(lastSubModule, bottomCategory);
      }
      Category blindZoneCategory = configurator.getSpecialCategory(Configurator.BLIND_ZONE_CATEGORY_NAME);
      if (blindZoneCategory.isActive()) {
         int kHz = Utils.hzToKHz(configurator.getReferenceFrequency());
         Optional<Float> blindZone = transducerParameterManager.getBlindZone(kHz);
         if (blindZone.isEmpty()) {
            throw new ModuleConfigurationException(module, "No range configured for " + kHz + " kHz in file " + transducerRangesFile);
         }
         lastSubModule = new BlindZoneCategorization(lastSubModule, blindZoneCategory, blindZone.get());
      }
      Category noiseCategory = configurator.getSpecialCategory(Configurator.NOISE_CATEGORY_NAME);
      if (noiseCategory.isActive()) {
         lastSubModule = new NoiseCategorization(lastSubModule, noiseCategory);
      }
      Category unknownCategory = configurator.getSpecialCategory(Configurator.UNKNOWN_CATEGORY_NAME);
      if (module.useMinLogSv.getBooleanValue()) {
         lastSubModule = new ThresholdCategorization(lastSubModule, unknownCategory, module.minLogSv.getFloatValue());
      }
      featureExtractSubModule = new FeatureExtractSubModule(lastSubModule, configurator);
      lastSubModule = featureExtractSubModule;
      lastSubModule = module.categorizer.getValue().factory.makeSubModule(lastSubModule, configurator);
      lastSubModule = module.discriminant.getValue().factory.makeSubModule(lastSubModule, configurator);
      if (module.contextualIterations.isEnabled()) {
         for (int i = 0; i < module.contextualIterations.getIntValue(); i++) {
            lastSubModule = module.contextualCorrection.getValue().factory.makeSubModule(lastSubModule, configurator);
            lastSubModule = module.discriminant.getValue().factory.makeSubModule(lastSubModule, configurator);
         }
      }

      lastSubModule = new MakeCad0SubModule(lastSubModule, module.categoryCount.getIntValue(), unknownCategory);

      Utils.getAllOfType(featureExtractSubModule.getFeatureExtractors(), FeatureExtractor.FrequencyFeatureExtractor.class).forEach(extractor -> {
         int kHz = extractor.getKHz();
         if (transducerParameterManager.getRange(kHz).isEmpty()) {
            Log.global.warning("No range configured for " + kHz + " kHz in file " + transducerRangesFile);
         }
      });

      PingConfiguration newPingConfiguration = configurator.configure(module, pingConfiguration);
      setNewPingConfiguration(newPingConfiguration);
   }

   Configurator getConfigurator() {
      return configurator;
   }

   FeatureExtractSubModule getFeatureExtractSubModule() {
      return featureExtractSubModule;
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      ExtendedPing extendedPing = lastSubModule.nextExtendedPing();
      return extendedPing != null ? extendedPing.getPing() : null;
   }

   private final class FirstSubModule implements CategorizationSubModule {
      private FirstSubModule() {
      }

      @Override
      public @Nullable ExtendedPing nextExtendedPing() throws IOException {
         Ping ping = inputPing();
         if (ping == null) {
            return null;
         }
         return new ExtendedPing(ping, configurator);
      }
   }
}
