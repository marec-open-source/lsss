package no.imr.korona.computation.categorization;

import no.imr.korona.computation.feature.Feature;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.ResampledFloatArray;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Extracts and sets the features of a pixel.
 */
final class FeatureExtractSubModule extends SimpleSubModule {
   private int channelsIn;
   private List<FeatureExtractor> featureExtractors = List.of();

   FeatureExtractSubModule(CategorizationSubModule previousSubModule, Configurator configurator) {
      super(previousSubModule);

      configure(configurator);
   }

   List<FeatureExtractor> getFeatureExtractors() {
      return featureExtractors;
   }

   void configure(Configurator configurator) {
      RawFileConfiguration rawFileConfiguration = configurator.getRawFileConfiguration();
      channelsIn = rawFileConfiguration != null ? rawFileConfiguration.getTransducerCount() : 0;
      featureExtractors = configurator.getOperationalFeatureExtractors();
   }

   @Override
   void process(ExtendedPing extendedPing, CategorizationPing categorizationPing) {
      PowerData referenceDatagram = categorizationPing.getReferenceDatagram();
      @Nullable ResampledFloatArray[] resampledFloatArrays = new ResampledFloatArray[channelsIn];
      for (int channel = 1; channel <= channelsIn; channel++) {
         PowerData powerData = extendedPing.getPing().getPowerData(channel);
         if (powerData != null) {
            resampledFloatArrays[channel - 1] = ResampledFloatArray.create(powerData.getSv(), powerData, referenceDatagram);
         }
      }
      Pixel[] pixels = categorizationPing.getPixels();

      for (int i = 0; i < pixels.length; i++) {
         Pixel pixel = pixels[i];
         if (!pixel.isDone()) {
            for (FeatureExtractor featureExtractor : featureExtractors) {
               Feature feature = featureExtractor.extract(resampledFloatArrays, referenceDatagram, i, extendedPing.getPing());
               if (feature != null) {
                  pixel.addFeature(feature);
               }
            }
         }
      }
   }
}
