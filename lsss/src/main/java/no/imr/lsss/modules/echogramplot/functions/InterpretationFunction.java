package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.region.LayerManager;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.ThresholdManager;
import no.imr.korona.region.storing.StoringConfigManager;
import no.imr.korona.region.storing.StoringIntervalConfig;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.LSSS;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

import java.util.List;

public final class InterpretationFunction {
   private InterpretationFunction() {
   }

   public static PingFunction bubbleCorrection(LSSS lsss) {
      RegionManager regionManager = lsss.getRegionManager();
      return new PingFunction(new Name("bubbleCorrection", "Interpretation: Bubble correction"), Unit.DIMENSIONLESS, ExportTransform.identity()) {
         @Override
         public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
            listenerRegistry.add(getChangeManager(), List.of(
                  regionManager.getBubbleCorrectionManager().getChangeManager()
            ));
         }

         @Override
         public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
            return regionManager.getBubbleCorrectionManager().getBubbleCorrection(ping.getPingIndex());
         }
      };
   }

   public static PingFunction lowerLayerBoundary(LSSS lsss) {
      LayerManager layerManager = lsss.getRegionManager().getLayerManager();
      return new PingFunction(new Name("lowerLayerBoundary", "Interpretation: Lower layer boundary"), Unit.METER, ExportRounding.depth()) {
         @Override
         public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
            listenerRegistry.add(getChangeManager(), List.of(
                  lsss.getRegionManager().getRegionBoundaryChangeManager()
            ));
         }

         @Override
         public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
            return layerManager.getBoundaryDepthRange(ping.getPingIndex()).max();
         }
      };
   }

   public static PingFunction lowerThreshold(LSSS lsss) {
      ThresholdManager thresholdManager = lsss.getRegionManager().getThresholdManager();
      return new PingFunction(new Name("lowerThreshold", "Interpretation: Lower threshold"), Unit.DB, ExportRounding.db()) {
         @Override
         public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
            listenerRegistry.add(getChangeManager(), List.of(
                  thresholdManager.getChangeManager()
            ));
         }

         @Override
         public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
            return thresholdManager.getLogSvRange(ping.getPingIndex()).min();
         }
      };
   }

   public static PingFunction quality(LSSS lsss) {
      StoringConfigManager storingConfigManager = lsss.getRegionManager().getStoringConfigManager();
      return new PingFunction(new Name("quality", "Interpretation: Quality"), Unit.DIMENSIONLESS, ExportTransform.identity()) {
         @Override
         public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
            listenerRegistry.add(getChangeManager(), List.of(
                  storingConfigManager.getChangeManager()
            ));
         }

         @Override
         public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
            StoringIntervalConfig intervalConfig = storingConfigManager.getConfigMap().get(ping.getPingIndex());
            return intervalConfig != null ? intervalConfig.quality() : Double.NaN;
         }
      };
   }
}
