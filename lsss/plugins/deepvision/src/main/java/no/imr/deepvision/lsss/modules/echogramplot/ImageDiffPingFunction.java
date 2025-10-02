package no.imr.deepvision.lsss.modules.echogramplot;

import no.imr.deepvision.lsss.engine.DeepVisionEngine;
import no.imr.deepvision.lsss.engine.ImageDiffCache;
import no.imr.deepvision.lsss.engine.data.DeepVisionDataUtils;
import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.deepvision.lsss.engine.data.DeepVisionFrameInfo;
import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFrame;
import no.imr.deepvision.lsss.engine.mapping.DeepVisionMapping;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.modules.echogramplot.functions.PingFunction;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import org.jfree.chart.plot.XYPlot;

import java.util.List;
import java.util.OptionalDouble;

public final class ImageDiffPingFunction extends PingFunction {
   private final DeepVisionEngine deepVisionEngine;
   private final SelectedFrameMarker selectedFrameMarker;
   private final ImageDiffCache imageDiffCache;
   private final Listener notifier = Listeners.coalescingDelayed(1000, getChangeManager());

   public ImageDiffPingFunction(DeepVisionEngine deepVisionEngine, SelectedFrameMarker selectedFrameMarker) {
      super(new Name("deepVisionImageDiff", "Deep Vision: Image diff"), Unit.NONE, ExportTransform.identity());

      this.deepVisionEngine = deepVisionEngine;
      this.selectedFrameMarker = selectedFrameMarker;
      imageDiffCache = deepVisionEngine.getImageDiffCache();
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
      List<DeepVisionFrame> frames = fileInfo.getDeepVisionFile().frames.frames;
      long deepVisionTime = deepVisionMapping.lsssTimeToDeepVisionTime(lsssTime, fileInfo);
      int i = Utils.binarySearchForLong(frames, deepVisionTime, DeepVisionDataUtils::timeInMillis);
      if (i < 0) {     // Not exact match
         i = -(i + 1); // Insertion index
         i--;          // Select previous
      }
      if (i < 0 || i >= frames.size()) {
         return Double.NaN;
      }
      DeepVisionFrameInfo frameInfo = new DeepVisionFrameInfo(fileInfo, i);
      OptionalDouble diff = imageDiffCache.getImageDiffIfPresent(frameInfo);
      if (diff.isPresent()) {
         return diff.getAsDouble();
      }
      Exec.LONG_RUNNING_THREAD_POOL.execute(() -> {
         computeInBackground(frameInfo);
      });
      return Float.NaN;
   }

   private void computeInBackground(DeepVisionFrameInfo frameInfo) {
      PingRange pingRange = deepVisionEngine.getLSSS().getInterpretationSettings().getPingRange();
      DeepVisionMapping deepVisionMapping = deepVisionEngine.getDeepVisionMappingManager().getDeepVisionMapping();
      long deepVisionTime = DeepVisionDataUtils.timeInMillis(frameInfo.frame());
      long lsssTime = deepVisionMapping.deepVisionTimeToLsssTime(deepVisionTime, frameInfo.deepVisionFileInfo());
      if (pingRange.containsTimeInMillis(lsssTime)) {
         imageDiffCache.getImageDiff(frameInfo);
         notifier.listen();
      }
   }
}
