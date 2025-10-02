package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.LSSS;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.Mean;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

import java.util.List;

public final class RelativeFrequencyResponseFunction extends PingFunction {
   private final LSSS lsss;
   private final RegionManager regionManager;

   public RelativeFrequencyResponseFunction(LSSS lsss) {
      super(new Name("relativeFrequencyResponse", "R(f)"), Unit.DB, ExportRounding.db(), true);

      this.lsss = lsss;
      regionManager = lsss.getRegionManager();
   }

   @Override
   public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
      listenerRegistry.add(getChangeManager(), List.of(
            lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency,
            regionManager.selectedRegions(),
            regionManager.getRegionDefinitionChangeManager(),
            regionManager.getThresholdManager().getChangeManager()
      ));
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      float mainFrequency = lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue();
      int mainChannel = dataFileSet.firstChannelClosestTo(mainFrequency);
      if (mainChannel < 1) {
         return Double.NaN;
      }
      List<Region> selectedRegions = regionManager.getSelectedRegions();
      FloatRange svRange = regionManager.getThresholdManager().getLinearSvRange(ping.getPingIndex());
      Mean mainSvSum = computeAverage(selectedRegions, ping, mainChannel, svRange);
      Mean currentSvSum = computeAverage(selectedRegions, ping, channel, svRange);
      if (mainSvSum.getCount() == 0 || currentSvSum.getCount() == 0) {
         return Double.NaN;
      }
      return KoronaUtils.toDB(currentSvSum.getMean() / mainSvSum.getMean());
   }

   private Mean computeAverage(List<Region> regions, Ping ping, int channel, FloatRange svRange) {
      Mean mean = new Mean();
      PowerData currentPowerData = ping.getPowerData(channel);
      if (currentPowerData != null) {
         float[] currentSv = currentPowerData.getSv();
         for (Region region : regions) {
            for (FloatRange depthRange : regionManager.getDepthRangesForChannel(region, ping, channel)) {
               int iBegin = currentPowerData.depthToClampedSampleIndex(depthRange.min());
               int iEnd = currentPowerData.depthToClampedSampleIndex(depthRange.max());
               for (int i = iBegin; i < iEnd; i++) {
                  float sv = currentSv[i];
                  if (svRange.contains(sv)) {
                     mean.update(sv);
                  }
               }
            }
         }
      }
      return mean;
   }
}
