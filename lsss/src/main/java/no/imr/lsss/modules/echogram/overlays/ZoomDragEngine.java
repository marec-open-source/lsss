package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.range.DoubleRange;
import no.imr.tools.range.FloatRange;

import java.awt.event.MouseEvent;

public final class ZoomDragEngine extends ZoomBaseEngine {
   private final InterpretationSettings interpretationSettings;
   private final EchogramZSettings zSettings;

   private final int referenceX;
   private final DoubleRange referenceValueRange;
   private final DoubleRange totalValueRange;

   private final int referenceY;
   private final FloatRange referenceZRange;
   private final FloatRange totalZRange;

   public ZoomDragEngine(MouseEvent mouseEvent, InterpretationSettings interpretationSettings, EchogramZSettings zSettings) {
      this.interpretationSettings = interpretationSettings;
      this.zSettings = zSettings;

      referenceX = mouseEvent.getX();
      referenceValueRange = interpretationSettings.getValueRange();
      PingRange totalRange = interpretationSettings.getDataFileSet().getTotalRange();
      totalValueRange = interpretationSettings.getPingMapping().toValueRange(totalRange);

      referenceY = mouseEvent.getY();
      referenceZRange = zSettings.getZoomedZRange();
      totalZRange = zSettings.getMaxZRange();
   }

   @Override
   public void update(MouseEvent mouseEvent) {
      interpretationSettings.getNavigationHistory().coalesceCheckPoint(this, () -> {
         int dx = referenceX - mouseEvent.getX();
         DoubleRange valueRange = referenceValueRange.shift((double) dx / interpretationSettings.getPingSettings().getWidth())
               .shiftToBeContainedIn(totalValueRange);
         interpretationSettings.setValueRange(valueRange);

         int dy = referenceY - mouseEvent.getY();
         FloatRange zRange = referenceZRange.shift((float) dy / zSettings.getHeight())
               .shiftToBeContainedIn(totalZRange);
         zSettings.setZ(zRange);
      });
   }
}
