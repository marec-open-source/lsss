package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.geometry.depth.CoordinatedBottomDepthTransform;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.geometry.depth.PerPingDepthTransform;
import no.imr.korona.region.Region;
import no.imr.korona.region.storing.StoringIntervalConfig;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.config.survey.GridConf;
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticCategoryConf;
import no.imr.lsss.framework.config.survey.data.DataType;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class GridParameters {
   final InterpretationModule interpretationModule;
   final LSSS lsss;
   final Map<Integer, AcousticCategory> acousticCategoryMap;
   final @Nullable AcousticCategory rawDataAcousticCategory;
   final PingMapping gridPingMapping;
   final FloatRange bottomInterpretationZRange;

   GridParameters(InterpretationModule interpretationModule) {
      this.interpretationModule = interpretationModule;
      lsss = interpretationModule.getLSSS();
      AcousticCategoryConf acousticCategoryConf = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf();
      acousticCategoryMap = acousticCategoryConf.getAcousticCategoryMap();
      rawDataAcousticCategory = acousticCategoryConf.getRawDataAcousticCategory();
      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      gridPingMapping = gridConf.horizontalGridUnit.getValue();
      bottomInterpretationZRange = FloatRange.of(-gridConf.verticalExtentBottom.getFloatValue(), 0);
   }

   static final class PerInterval {
      final GridParameters gridParameters;
      final PingRange pingRange;
      final StoringIntervalConfig intervalConfig;
      final DataFileSet dataFileSet;
      final Set<Integer> channels;
      private final DepthTransform bottomDepthTransform;

      PerInterval(GridParameters gridParameters, InterpretationModule.StoreInput storeInput) {
         this.gridParameters = gridParameters;
         pingRange = storeInput.gridColumnInterval().pingRange();
         intervalConfig = storeInput.intervalConfig();
         DataManager dataManager = dataManager(gridParameters.lsss, intervalConfig);
         dataFileSet = dataManager.getDataFileSet();
         channels = InterpretationModule.kHzsToChannels(dataFileSet, intervalConfig.kHz());
         bottomDepthTransform = new CoordinatedBottomDepthTransform(dataManager);
      }

      private static DataManager dataManager(LSSS lsss, StoringIntervalConfig intervalConfig) {
         DataType dataType = intervalConfig.dataDir() == StoringIntervalConfig.DATA_DIR_RAW ? DataType.RAW : DataType.PROCESSED;
         return lsss.getDataSetManager().getDataManager(dataType);
      }
   }

   static final class PerPing {
      final PerInterval perInterval;
      final FloatRange svRange;
      private final PerPingDepthTransform pelagicPerPingDepthTransform;
      private final PerPingDepthTransform bottomPerPingDepthTransform;
      private final Map<Region, FloatRangeSet> nonMaskedRegionDepthRanges;
      final Map<Integer, PerChannel> perChannels;

      PerPing(PerInterval perInterval, Ping ping) {
         this.perInterval = perInterval;
         LSSS lsss = perInterval.gridParameters.lsss;
         svRange = lsss.getRegionManager().getThresholdManager().getLinearSvRange(ping.getPingIndex());
         pelagicPerPingDepthTransform = lsss.getInterpretationSettings().getPelagicZSettings().getDepthTransform().forPing(ping.getPingIndex());
         bottomPerPingDepthTransform = perInterval.bottomDepthTransform.forPing(ping.getPingIndex());
         nonMaskedRegionDepthRanges = lsss.getRegionManager().getNonMaskedRegionDepthRanges(ping.getPingIndex());
         perChannels = perInterval.channels.stream().collect(Collectors.toUnmodifiableMap(
               Function.identity(),
               channel -> new PerChannel(this, ping, channel)));
      }

      PerPingDepthTransform getPerPingDepthTransform(Grid grid) {
         return grid.isPelagic() ? pelagicPerPingDepthTransform : bottomPerPingDepthTransform;
      }
   }

   static final class PerChannel {
      private final PerPing perPing;
      final @Nullable PowerData powerData;
      private final FloatRangeSet maskedDepthRanges;

      private PerChannel(PerPing perPing, Ping ping, int channel) {
         this.perPing = perPing;
         powerData = ping.getPowerData(channel);
         maskedDepthRanges = perPing.perInterval.gridParameters.lsss.getRegionManager().getMaskedDepthRanges(ping, channel);
      }

      FloatRangeSet getDepthRanges(Region region) {
         FloatRangeSet regionDepthRanges = perPing.nonMaskedRegionDepthRanges.get(region);
         if (regionDepthRanges == null) {
            return FloatRangeSet.of();
         }
         return regionDepthRanges.subtract(maskedDepthRanges);
      }
   }
}
