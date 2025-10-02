package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.RegionConfiguration;
import no.imr.lsss.LSSS;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.RangeSet;
import no.imr.tools.range.RangeUtils;

import java.util.concurrent.Executor;

/**
 * The region configuration for LSSS.
 */
public class LsssRegionConfiguration implements RegionConfiguration {
   public static final float PELAGIC_DEPTH_FACTOR = 0.9f;

   private final LSSS lsss;
   private boolean readOnlyPingsEnabled = true;

   public LsssRegionConfiguration(LSSS lsss) {
      this.lsss = lsss;
   }

   protected DataFileSet getDataFileSet() {
      return lsss.getDataManager().getDataFileSet();
   }

   @Override
   public PingContainer getPingContainer() {
      return getDataFileSet();
   }

   @Override
   public DataConfiguration getDataConfiguration() {
      return getDataFileSet().getDataConfiguration();
   }

   @Override
   public PingRange getVisiblePingRange() {
      return lsss.getInterpretationSettings().getPingRange();
   }

   @Override
   public ToFloatFunction<PingIndex> initialUpperDepth() {
      float depth;
      DataConfiguration dataConfiguration = getDataFileSet().getDataConfiguration();
      if (dataConfiguration.isSeabedMounted()) {
         float bottomBoundaryOffset = lsss.getConfigurationManager().getSurveyMiscConf().bottomBoundaryOffset.getFloatValue();
         depth = bottomBoundaryOffset - dataConfiguration.getSeabedMountedDistanceToSeabed();
      } else {
         depth = lsss.getConfigurationManager().getSurveyMiscConf().topBoundaryOffset.getFloatValue();
      }
      return __ -> depth;
   }

   @Override
   public ToFloatFunction<PingIndex> initialLowerDepth() {
      float pelagicDepth = PELAGIC_DEPTH_FACTOR * getDataFileSet().getMaxDepth();
      return initialLowerDepth(pelagicDepth, true);
   }

   public ToFloatFunction<PingIndex> initialLowerDepth(float pelagicDepth, boolean useCoordinatedBottom) {
      DataFileSet dataFileSet = getDataFileSet();
      DataConfiguration dataConfiguration = dataFileSet.getDataConfiguration();
      if (dataConfiguration.isSeabedMounted()) {
         float topBoundaryOffset = lsss.getConfigurationManager().getSurveyMiscConf().topBoundaryOffset.getFloatValue();
         float depth = dataConfiguration.getSeabedMountedDistanceToSurface() - topBoundaryOffset;
         return __ -> depth;
      }
      if (lsss.getConfigurationManager().getSurveyMiscConf().pelagicMode.getBooleanValue()) {
         return __ -> pelagicDepth;
      }
      int channelIndex = lsss.getInterpretationSettings().getChannel() - 1;
      float bottomBoundaryOffset = lsss.getConfigurationManager().getSurveyMiscConf().bottomBoundaryOffset.getFloatValue();
      return pingIndex -> {
         float bottomDepth = useCoordinatedBottom
               ? dataFileSet.getCoordinatedDepth(pingIndex)
               : (float) dataFileSet.getBot0Datagram(pingIndex).getChannelDepths()[channelIndex];
         return bottomDepth - bottomBoundaryOffset;
      };
   }

   @Override
   public FloatRange getDefaultThresholds() {
      float min = lsss.getConfigurationManager().getSurveyMiscConf().preferredLowerThreshold.getIntValue();
      float max = lsss.getConfigurationManager().getSurveyMiscConf().preferredUpperThreshold.getIntValue();
      max = Math.max(max, min);
      return FloatRange.of(min, max);
   }

   @Override
   public int nextObjectNumber() {
      return lsss.getSurveyManager().getWorkData().nextObjectNumber();
   }

   @Override
   public Executor getBackgroundExecutor() {
      return lsss.getInterpretationSettings().getObservingExecutor();
   }

   @Override
   public void setReadOnlyPingsEnabled(boolean readOnlyPingsEnabled) {
      this.readOnlyPingsEnabled = readOnlyPingsEnabled;
   }

   @Override
   public RangeSet<PingIndex> getReadOnlyPings() {
      return readOnlyPingsEnabled ? lsss.getInterpretationSummary().getStoredPings() : RangeUtils.emptyRangeSet();
   }
}
