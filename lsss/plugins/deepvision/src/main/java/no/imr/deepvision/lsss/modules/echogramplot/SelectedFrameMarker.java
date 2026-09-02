package no.imr.deepvision.lsss.modules.echogramplot;

import no.imr.deepvision.lsss.engine.DeepVisionEngine;
import no.imr.deepvision.lsss.engine.data.DeepVisionDataUtils;
import no.imr.deepvision.lsss.engine.data.DeepVisionFrameInfo;
import no.imr.deepvision.lsss.modules.echogram.DeepVisionPathEchogramOverlay;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.swing.GuiUtils;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.Layer;

import java.time.Instant;

public final class SelectedFrameMarker {
   private final DeepVisionEngine deepVisionEngine;
   private ValueMarker selectedFrameMarker = new ValueMarker(Double.NaN);

   public SelectedFrameMarker(DeepVisionEngine deepVisionEngine) {
      this.deepVisionEngine = deepVisionEngine;

      deepVisionEngine.getDeepVisionSelectedFrame().getChangeManager().addListener(this::update);
   }

   private void update() {
      double value;
      DeepVisionFrameInfo selectedFrame = deepVisionEngine.getDeepVisionSelectedFrame().getSelectedFrame();
      if (selectedFrame == null) {
         value = Double.NaN;
      } else {
         Instant deepVisionTime = DeepVisionDataUtils.time(selectedFrame.frame());
         Instant lsssTime = deepVisionEngine.getDeepVisionMappingManager().getDeepVisionMapping().deepVisionTimeToLsssTime(deepVisionTime, selectedFrame.deepVisionFileInfo());
         InterpretationSettings interpretationSettings = deepVisionEngine.getLSSS().getInterpretationSettings();
         float x = interpretationSettings.getPingSettings().instantToX(lsssTime);
         value = interpretationSettings.getPingSettings().xToValue(x, interpretationSettings.getPingMapping());
      }
      selectedFrameMarker.setValue(value);
   }

   void addTo(XYPlot plot) {
      for (Object marker : plot.getDomainMarkers(Layer.FOREGROUND)) {
         if (marker == selectedFrameMarker) { // Test object identity. ValueMarker tests for equal value.
            return;
         }
      }
      // Create new marker to avoid memory leak, #1423.
      selectedFrameMarker = new ValueMarker(Double.NaN, DeepVisionPathEchogramOverlay.SELECTED_FRAME_COLOR, GuiUtils.STROKE_1);
      update();
      plot.addDomainMarker(selectedFrameMarker);
   }
}
