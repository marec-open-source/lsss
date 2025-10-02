package no.imr.deepvision.lsss.modules.echogramplot;

import no.imr.deepvision.lsss.engine.DeepVisionEngine;
import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.deepvision.lsss.engine.mapping.DeepVisionMapping;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.lsss.modules.echogramplot.functions.PingFunction;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import org.jfree.chart.plot.XYPlot;

public final class AthwartDistancePingFunction extends PingFunction {
   private final DeepVisionEngine deepVisionEngine;
   private final SelectedFrameMarker selectedFrameMarker;

   public AthwartDistancePingFunction(DeepVisionEngine deepVisionEngine, SelectedFrameMarker selectedFrameMarker) {
      super(new Name("deepVisionAthwartDistance", "Deep Vision: Athwart distance"), Unit.METER, ExportTransform.round(100));

      this.deepVisionEngine = deepVisionEngine;
      this.selectedFrameMarker = selectedFrameMarker;
      deepVisionEngine.getDeepVisionMappingManager().getChangeManager().addListener(getChangeManager());
   }

   @Override
   public void addMarkers(XYPlot plot) {
      selectedFrameMarker.addTo(plot);
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      long lsssTime = ping.getTimeInMillis();
      DeepVisionMapping deepVisionMapping = deepVisionEngine.getDeepVisionMappingManager().getDeepVisionMapping();
      DeepVisionFileInfo fileInfo = deepVisionEngine.getDataAdministrator().lsssTimeToFileInfo(lsssTime, deepVisionMapping);
      if (fileInfo == null) {
         return Double.NaN;
      }
      long deepVisionTime = deepVisionMapping.lsssTimeToDeepVisionTime(lsssTime, fileInfo);
      return deepVisionMapping.deepVisionTimeToAthwartDistanceMeters(deepVisionTime, fileInfo);
   }
}
