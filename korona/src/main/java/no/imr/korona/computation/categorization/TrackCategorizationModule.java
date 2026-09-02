package no.imr.korona.computation.categorization;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.categorization.apriori.PerPingAPriori;
import no.imr.korona.computation.feature.Feature;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.datagrams.Cat0Datagram;
import no.imr.korona.data.datagrams.TBR0Datagram;
import no.imr.korona.data.datagrams.TNF0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Utils;
import no.imr.tools.math.Mean;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TrackCategorizationModule extends SimplePingModule {
   public TrackCategorizationModule() {
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(CategorizationFileService.NAME, TransducerRangesFileService.NAME);
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new TrackCategorizationModuleComputation(this, computationContext, pingSource);
   }

   private static final class TrackCategorizationModuleComputation extends SimplePingModuleComputation {
      private final Map<Integer, TrackAccumulation> trackAccumulations = new HashMap<>();
      private final Configurator configurator;
      private final FeatureExtractor.@Nullable FrequencyFeatureExtractor[] frequencyFeatureExtractors;
      private final boolean useSv38;
      private final GaussCategorizer gaussCategorizer;
      private final Category unknownCategory;

      private TrackCategorizationModuleComputation(TrackCategorizationModule module, ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
         super(module, computationContext, pingSource);

         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         configurator = new Configurator(computationContext.getModuleContainer().getConfigFileSettings(), pingConfiguration.getRawFileConfiguration());
         configurator.setCategoryType(Category.Type.Track);

         frequencyFeatureExtractors = new FeatureExtractor.FrequencyFeatureExtractor[pingConfiguration.getRawFileConfiguration().getTransducerCount() + 1];
         Utils.getAllOfType(configurator.getOperationalFeatureExtractors(), FeatureExtractor.FrequencyFeatureExtractor.class).forEach(ffe -> {
            int channel = pingConfiguration.getRawFileConfiguration().lastChannelWithKHz(ffe.getKHz());
            frequencyFeatureExtractors[channel] = ffe;
         });

         FeatureExtractor sv38FeatureExtractor = configurator.getFeatureExtractor(FeatureExtractor.ADDITIONAL_FEATURE_SV38);
         useSv38 = sv38FeatureExtractor != null && sv38FeatureExtractor.isOperational();

         gaussCategorizer = new GaussCategorizer(Category.DistributionLevel.SCHOOL, false, configurator);

         unknownCategory = configurator.getSpecialCategory(Configurator.UNKNOWN_CATEGORY_NAME);

         PingConfiguration newPingConfiguration = configurator.configure(module, pingConfiguration);
         setNewPingConfiguration(newPingConfiguration);
      }

      @Override
      protected void convertPing(Ping ping, Ping newPing) {
         newPing.addAll(ping.getPingItems());
      }

      @Override
      protected void processPing(Ping ping) {
         ping.removeAll(Cat0Datagram.class);

         ping.getPingItems(TBR0Datagram.class).forEach(tbr0Datagram -> {
            getTrackAccumulation(tbr0Datagram.getId()).accumulate(ping, tbr0Datagram);
         });

         PerPingAPriori perPingAPriori = null;
         for (TNF0Datagram tnf0Datagram : ping.getPingItems(TNF0Datagram.class).toList()) {
            if (tnf0Datagram.isValid()) {
               if (perPingAPriori == null) {
                  perPingAPriori = new PerPingAPriori(configurator, ping);
               }
               categorize(ping, tnf0Datagram, perPingAPriori);
            }
            trackAccumulations.remove(tnf0Datagram.getId());
         }
      }

      private void categorize(Ping ping, TNF0Datagram tnf0Datagram, PerPingAPriori perPingAPriori) {
         TrackAccumulation trackAccumulation = getTrackAccumulation(tnf0Datagram.getId());
         Pixel pixel = trackAccumulation.createPixel(useSv38, frequencyFeatureExtractors, configurator.getReferenceChannel());

         if (pixel.getFeatures().isEmpty()) {
            pixel.setFinalCategory(unknownCategory);
         } else {
            gaussCategorizer.categorize(pixel, perPingAPriori);
         }

         for (CategoryData cd : pixel.getCategoryDatas()) {
            //a priori discriminant
            cd.setDiscriminant(cd.getNormalizedProbability() * cd.getTotalApriori());
         }
         pixel.normalizeDiscriminants();

         List<CategoryData> categoryDatas = pixel.getAcceptableCategoryDatas();
         if (categoryDatas.isEmpty()) {
            pixel.setFinalCategory(unknownCategory);
            categoryDatas = pixel.getAcceptableCategoryDatas();
         }

         Cat0Datagram cat0Datagram = new Cat0Datagram(ping.getInstant(), categoryDatas.size(), tnf0Datagram.getId());
         categoryDatas.sort(null);
         int priority = 0;
         for (CategoryData categoryData : categoryDatas) {
            cat0Datagram.setCategory(priority, categoryData.getCategory().getNumber(), categoryData.getDiscriminant(), categoryData.getBoundedProbability());
            priority++;
         }
         ping.add(cat0Datagram);
      }

      private TrackAccumulation getTrackAccumulation(Integer id) {
         TrackAccumulation trackAccumulation = trackAccumulations.get(id);
         if (trackAccumulation == null) {
            trackAccumulation = new TrackAccumulation(getPingConfiguration().getRawFileConfiguration().getTransducerCount());
            trackAccumulations.put(id, trackAccumulation);
         }
         return trackAccumulation;
      }

      private static final class TrackAccumulation {
         private final Mean[] linearTscMeans;

         private TrackAccumulation(int transducerCount) {
            linearTscMeans = new Mean[transducerCount + 1];
            for (int i = 0; i < linearTscMeans.length; i++) {
               linearTscMeans[i] = new Mean();
            }
         }

         private void accumulate(Ping ping, TBR0Datagram tbr0Datagram) {
            FloatRange depthRange = tbr0Datagram.getDepthRange();
            ping.getNonNullPowerDatas().forEach(powerData -> {
               Mean mean = linearTscMeans[powerData.getChannel()];
               int iBegin = powerData.depthToClampedSampleIndex(depthRange.min());
               int iEnd = powerData.depthToClampedSampleIndex(depthRange.max());
               for (int i = iBegin; i < iEnd; i++) {
                  mean.update(powerData.getLinearTSC(i));
               }
            });
         }

         private Pixel createPixel(boolean useSv38, FeatureExtractor.@Nullable FrequencyFeatureExtractor[] frequencyFeatureExtractors, int referenceChannel) {
            if (linearTscMeans[referenceChannel].getCount() == 0) {
               return new Pixel(Float.NaN);
            }
            double referenceValue = linearTscMeans[referenceChannel].getMean();
            Pixel pixel = new Pixel(PowerData.svToLogSv((float) referenceValue));

            for (int channel = 1; channel < linearTscMeans.length; channel++) {
               FeatureExtractor.FrequencyFeatureExtractor ffe = frequencyFeatureExtractors[channel];
               if (ffe == null) {
                  continue;
               }
               if (linearTscMeans[channel].getCount() == 0) {
                  continue;
               }
               double channelValue = linearTscMeans[channel].getMean();
               pixel.addFeature(new Feature(ffe.getFeatureName(), KoronaUtils.toDB(channelValue / referenceValue)));
            }

            if (useSv38) {
               pixel.addFeature(new Feature(FeatureExtractor.ADDITIONAL_FEATURE_SV38, KoronaUtils.toDB(referenceValue)));
            }

            return pixel;
         }
      }
   }
}
