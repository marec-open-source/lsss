package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.overlays.MaskingDisplayOverlay;
import no.imr.lsss.util.PingExclusion;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class ExclusionPhantomOverlay extends BasePhantomOverlay {
   private static final Color EXCLUSION_COLOR = new Color(0x66000000, true);

   private final PingExclusion pingExclusion;

   public ExclusionPhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule, PingExclusion pingExclusion) {
      super(moduleInfo, phantomEchogramModule);

      this.pingExclusion = pingExclusion;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getPhantomEchogramModule().getEchogramAreaChangeManager(),
            pingExclusion.getChangeManager()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      RangeSet<Integer> exclusionRangesX = toX(pingExclusion.getExclusionRanges());
      RangeSet<Integer> exclusionSinglePingX = toX(pingExclusion.getExcludedSinglePings());
      if (exclusionRangesX.isEmpty() && exclusionSinglePingX.isEmpty()) {
         return null;
      }
      return new DisplayData(getHeight(), exclusionRangesX, exclusionSinglePingX);
   }

   private RangeSet<Integer> toX(RangeSet<PingIndex> exclusionRanges) {
      RangeSet<Integer> x = new ArrayRangeSet<>();
      for (Range<PingIndex> exclusionRange : exclusionRanges) {
         if (!exclusionRange.intersects(getPingSettings().getPingRange())) {
            continue;
         }
         int x0 = Math.max(getPingSettings().pingIndexToXIndex(exclusionRange.begin()), 0);
         int x1 = Math.min(getPingSettings().pingIndexToXIndex(exclusionRange.end()), getWidth());
         if (x0 >= x1) {
            continue;
         }
         x.add(x0, x1);
      }
      return x;
   }

   private record DisplayData(
         int height,
         RangeSet<Integer> exclusionRangesX,
         RangeSet<Integer> exclusionSinglePingX
   ) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(EXCLUSION_COLOR);
         MaskingDisplayOverlay.fill(g2d, exclusionRangesX, height);
         MaskingDisplayOverlay.fill(g2d, exclusionSinglePingX, height);

         g2d.setColor(Color.BLACK);
         MaskingDisplayOverlay.drawSlopingLines(g2d, exclusionRangesX, height);
         MaskingDisplayOverlay.drawSlopingLines(g2d, exclusionSinglePingX, height);
      }
   }
}
