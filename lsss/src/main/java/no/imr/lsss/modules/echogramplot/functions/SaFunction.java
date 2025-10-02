package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.LSSS;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

import java.util.List;

public final class SaFunction extends PingFunction {
   private final RegionManager regionManager;

   public SaFunction(LSSS lsss) {
      super(new Name("sa", "sA"), Unit.SA, ExportRounding.sa(), true);

      regionManager = lsss.getRegionManager();
   }

   @Override
   public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
      listenerRegistry.add(getChangeManager(), List.of(
            regionManager.selectedRegions(),
            regionManager.getRegionDefinitionChangeManager(),
            regionManager.getThresholdManager().getChangeManager()
      ));
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null) {
         return Double.NaN;
      }
      FloatRange svRange = regionManager.getThresholdManager().getLinearSvRange(ping.getPingIndex());
      double svSum = 0;
      for (Region region : regionManager.getSelectedRegions()) {
         for (FloatRange depthRange : regionManager.getDepthRangesForChannel(region, ping, channel)) {
            svSum += powerData.getVerticalIntegralSv(depthRange, svRange);
         }
      }
      return svSum;
   }
}
