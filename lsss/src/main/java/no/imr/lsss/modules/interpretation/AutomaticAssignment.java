package no.imr.lsss.modules.interpretation;

import com.google.common.util.concurrent.AtomicDouble;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datagrams.Cat0Datagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticToCategory;
import no.imr.lsss.modules.korona.region.KoronaRegionLSSS;
import no.imr.lsss.modules.korona.region.KoronaRegionModule;
import no.imr.lsss.modules.korona.tracking.TrackId;
import no.imr.lsss.modules.korona.tracking.TrackInfo;
import no.imr.lsss.modules.korona.tracking.TrackInfoModule;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeMap;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class AutomaticAssignment {
   private final LSSS lsss;
   private final Collection<Region> regions;
   private final Collection<Integer> channels;

   private final DataFileSet dataFileSet;
   private final PingRange pingRange;
   private final CategoryVariable categoryVariable;
   private final Map<PingIndex, RangeMap<Float, String>> regionCategories = new HashMap<>();
   private final @Nullable TrackInfoModule trackInfoModule;

   AutomaticAssignment(LSSS lsss, Collection<Region> regions, Collection<Integer> channels) {
      this.lsss = lsss;
      this.regions = regions;
      this.channels = channels;

      dataFileSet = lsss.getInterpretationSettings().getDataFileSet();

      pingRange = regions.stream()
            .map(Region::getPingRange)
            .reduce(PingRange.EMPTY_RANGE, PingRange::union);

      categoryVariable = lsss.getInterpretationSettings().getColorConverterContainer().getDiscreteVariable(CategoryVariable.class);

      if (lsss.getConfigurationManager().getSurveyMiscConf().useSchoolCategorization.getBooleanValue()) {
         defineRegionCategories();
      }

      if (lsss.getConfigurationManager().getSurveyMiscConf().useTrackCategorization.getBooleanValue()) {
         trackInfoModule = lsss.getModuleManager().getModule(TrackInfoModule.class);
      } else {
         trackInfoModule = null;
      }
   }

   private void defineRegionCategories() {
      Cac0Datagram cac0Datagram = dataFileSet.getPingConfiguration().getConfigurationItem(Cac0Datagram.class);
      if (cac0Datagram == null) {
         return;
      }
      KoronaRegionModule koronaRegionModule = lsss.getModuleManager().getModule(KoronaRegionModule.class);
      for (KoronaRegionLSSS koronaRegion : koronaRegionModule.getKoronaRegions()) {
         PingRange koronaRegionPingRange = koronaRegion.getPingRange().intersection(pingRange);
         if (koronaRegionPingRange.isEmpty()) {
            continue;
         }
         Cas0Datagram cas0Datagram = koronaRegion.getCas0Datagram();
         if (cas0Datagram == null) {
            continue;
         }
         byte bestCategory = cas0Datagram.getBestCategory(categoryVariable, (byte) -1);
         if (bestCategory < 0) {
            continue;
         }
         Cac0Datagram.Category category = cac0Datagram.numberToCategory(bestCategory);
         for (PingIndex pingIndex : dataFileSet.getPingIndices(koronaRegionPingRange)) {
            FloatRangeSet depthRanges = koronaRegion.getMask().get(pingIndex);
            if (depthRanges != null) {
               RangeMap<Float, String> rangeMap = regionCategories.computeIfAbsent(pingIndex, _ -> new ArrayRangeMap<>());
               for (FloatRange depthRange : depthRanges) {
                  rangeMap.put(depthRange.min(), depthRange.max(), category.getName());
               }
            }
         }
      }
   }

   private @Nullable RangeMap<Float, String> getDepthToTrackCategory(Ping ping, Cac0Datagram cac0Datagram) {
      if (trackInfoModule == null) {
         return null;
      }
      RangeMap<Float, String> depthToTrackCategory = new ArrayRangeMap<>();
      Map<TrackId, TrackInfo> trackInfos = trackInfoModule.getTrackInfos();
      trackInfoModule.getTrackEditing().getTrackBorders(ping).forEach(trackBorder -> {
         TrackInfo trackInfo = trackInfos.get(trackBorder.trackId());
         if (trackInfo == null) {
            return;
         }
         Cat0Datagram cat0Datagram = trackInfo.cat0Datagram();
         if (cat0Datagram == null) {
            return;
         }
         byte categoryNumber = cat0Datagram.getBestCategory(categoryVariable, (byte) -1);
         if (categoryNumber < 0) {
            return;
         }
         Cac0Datagram.Category category = cac0Datagram.numberToCategory(categoryNumber);
         FloatRange depthRange = trackBorder.depthRange();
         depthToTrackCategory.put(depthRange.min(), depthRange.max(), category.getName());
      });
      return depthToTrackCategory;
   }

   void run(@Nullable Component referenceComponent) {
      Cac0Datagram cac0Datagram = dataFileSet.getPingConfiguration().getConfigurationItem(Cac0Datagram.class);
      if (cac0Datagram == null) {
         JOptionPane.showMessageDialog(referenceComponent, "Data files do not contain categorization");
         return;
      }
      ProgressView progressView = new ProgressView("Doing automatic interpretation...", 1000)
            .mainProgressAsPercentage();
      new WorkerDialog(referenceComponent, progressView.getComponent())
            .start(asyncHandle -> run(asyncHandle, progressView.getMainProgressHandler()));
   }

   void run(AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      Cac0Datagram cac0Datagram = dataFileSet.getPingConfiguration().getConfigurationItem(Cac0Datagram.class);
      if (cac0Datagram == null) {
         return;
      }

      RegionManager regionManager = lsss.getRegionManager();
      PingMapping pingMapping = lsss.getInterpretationSettings().getPingMapping();

      Listener progressListener = progressHandler.asCountingListener(pingRange.getPingCount());

      int maxChannel = channels.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
      Map<Region, RegionIntegration> regionToInfo = regions.stream()
            .collect(Collectors.toMap(Function.identity(), _ -> new RegionIntegration(maxChannel, channels)));

      for (PingIndex pingIndex : dataFileSet.getPingIndices(pingRange)) {
         Ping ping = dataFileSet.getPing(pingIndex);
         progressListener.listen();
         if (asyncHandle.isCancelled()) {
            return;
         }
         Cad0Datagram cad0Datagram = ping.getPingItem(Cad0Datagram.class);
         if (cad0Datagram == null) {
            continue;
         }
         RangeMap<Float, String> depthToRegionCategory = regionCategories.get(pingIndex);
         RangeMap<Float, String> depthToTrackCategory = getDepthToTrackCategory(ping, cac0Datagram);
         FloatRange svRange = regionManager.getThresholdManager().getLinearSvRange(pingIndex);

         for (int channel : channels) {
            PowerData powerData = ping.getPowerData(channel);
            if (powerData == null) {
               continue;
            }
            float[] svArray = powerData.getSv();

            for (Region region : regions) {
               FloatRangeSet depthRanges = regionManager.getDepthRangesForChannel(region, ping, channel);
               if (depthRanges.isEmpty()) {
                  continue;
               }
               RegionIntegration regionIntegration = regionToInfo.get(region);
               ChannelIntegration channelIntegration = regionIntegration.channelIntegrations[channel];
               double horizontalDistance = pingMapping.distance(pingIndex, dataFileSet.nextOrSame(pingIndex));
               double sampleArea = horizontalDistance * powerData.getSampleDistance();

               double svSum = 0;
               for (FloatRange depthRange : depthRanges) {
                  int beginIndex = powerData.depthToClampedSampleIndex(depthRange.min());
                  int endIndex = powerData.depthToClampedSampleIndex(depthRange.max());
                  for (int i = beginIndex; i < endIndex; i++) {
                     float sv = svArray[i];
                     if (!svRange.contains(sv)) {
                        continue;
                     }
                     svSum += sv;
                     float depth = powerData.getSampleDepth(i);

                     String koronaCategory = null;
                     if (depthToRegionCategory != null) {
                        koronaCategory = depthToRegionCategory.get(depth);
                     }
                     if (koronaCategory == null && depthToTrackCategory != null) {
                        koronaCategory = depthToTrackCategory.get(depth);
                     }
                     if (koronaCategory == null) {
                        byte categoryNumber = cad0Datagram.getBestCategory(cad0Datagram.depthToIndex(depth), categoryVariable, (byte) -1);
                        if (categoryNumber >= 0) {
                           Cac0Datagram.Category category = cac0Datagram.numberToCategory(categoryNumber);
                           koronaCategory = category.getName();
                        }
                     }
                     if (koronaCategory != null) {
                        channelIntegration.koronaCategoryToSvIntegral.computeIfAbsent(koronaCategory, _ -> new AtomicDouble())
                              .addAndGet(sv * sampleArea);
                     }
                  }
               }
               channelIntegration.totalSvIntegral += svSum * sampleArea;
            }
         }
      }

      Map<String, List<AcousticCategory>> koronaCategoryToAcousticCategory = findKoronaCategoryToAcousticCategories();

      for (Map.Entry<Region, RegionIntegration> regionEntry : regionToInfo.entrySet()) {
         RegionIntegration regionIntegration = regionEntry.getValue();
         for (int channel : channels) {
            ChannelIntegration channelIntegration = regionIntegration.channelIntegrations[channel];
            Map<Integer, AtomicDouble> acousticCategoryToSvIntegral = new HashMap<>();
            for (Map.Entry<String, AtomicDouble> koronaEntry : channelIntegration.koronaCategoryToSvIntegral.entrySet()) {
               List<AcousticCategory> acousticCategories = koronaCategoryToAcousticCategory.get(koronaEntry.getKey());
               if (acousticCategories == null) {
                  continue;
               }
               double svIntegralPerAcousticCategory = koronaEntry.getValue().get() / acousticCategories.size();
               for (AcousticCategory acousticCategory : acousticCategories) {
                  acousticCategoryToSvIntegral.computeIfAbsent(acousticCategory.getCompId().getAcousticCategory(), _ -> new AtomicDouble())
                        .addAndGet(svIntegralPerAcousticCategory);
               }
            }
            ChannelInterpretation channelInterpretation = regionEntry.getKey().getInterpretation().getChannelInterpretation(channel);
            channelInterpretation.reset();
            for (Map.Entry<Integer, AtomicDouble> acousticCategoryEntry : acousticCategoryToSvIntegral.entrySet()) {
               double assignment = acousticCategoryEntry.getValue().get() / channelIntegration.totalSvIntegral;
               channelInterpretation.setAssignment(acousticCategoryEntry.getKey(), (float) assignment);
            }
         }
      }

      regionManager.getInterpretationChangeManager().notifyListeners(this);
   }

   private Map<String, List<AcousticCategory>> findKoronaCategoryToAcousticCategories() {
      AcousticToCategory acousticToCategory = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticToCategory();
      Map<String, List<AcousticCategory>> koronaCategoryToAcousticCategories = new HashMap<>();
      for (Map.Entry<AcousticCategory, Set<String>> entry : acousticToCategory.getAcousticCategoryToKoronaCategories().entrySet()) {
         for (String koronaCategory : entry.getValue()) {
            koronaCategoryToAcousticCategories.computeIfAbsent(koronaCategory, _ -> new ArrayList<>()).add(entry.getKey());
         }
      }
      return koronaCategoryToAcousticCategories;
   }

   private static final class ChannelIntegration {
      private double totalSvIntegral;
      private final Map<String, AtomicDouble> koronaCategoryToSvIntegral = new HashMap<>();

      private ChannelIntegration() {
      }
   }

   private static final class RegionIntegration {
      private final ChannelIntegration[] channelIntegrations;

      private RegionIntegration(int maxChannel, Collection<Integer> channels) {
         channelIntegrations = new ChannelIntegration[maxChannel + 1];
         for (int channel : channels) {
            channelIntegrations[channel] = new ChannelIntegration();
         }
      }
   }
}
