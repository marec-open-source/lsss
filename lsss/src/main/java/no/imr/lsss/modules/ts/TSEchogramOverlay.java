package no.imr.lsss.modules.ts;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.region.Region;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.Supplier;

/**
 * Draws TS detections on the echogram.
 */
public final class TSEchogramOverlay extends BaseEchogramOverlay {
   private final Supplier<TSModule> tsModule = moduleSupplier(TSModule.class);

   public TSEchogramOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            tsModule.get().getTSDetectionChangeManager(),
            getEchogramModule().echogramArea()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      Path2D.Float path = new Path2D.Float();
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      int channel = getInterpretationSettings().getChannel();
      for (Region region : getRegionManager().getSelectedRegions()) {
         tsModule.get().getTSData(region).forEach((pingIndex, pingCache) -> {
            float x0 = getPingSettings().pingIndexToX(pingIndex);
            PingIndex nextPingIndex = dataFileSet.nextOrNull(pingIndex);
            float x1 = nextPingIndex != null ? Math.max(x0 + 1, getPingSettings().pingIndexToX(nextPingIndex)) : getWidth();

            for (TSData tsData : pingCache.getTsData(channel)) {
               float y = getZSettings().depthToY(tsData.depth(), pingIndex);
               if (y >= 0 && y <= getHeight()) {
                  path.moveTo(x0, y);
                  path.lineTo(x1, y);
               }
            }
         });
      }
      if (path.getCurrentPoint() == null) {
         return null;
      }
      return transformed(new DisplayData(path));
   }

   private record DisplayData(Path2D.Float path) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.BLACK);
         g2d.setStroke(GuiUtils.STROKE_2);
         g2d.draw(path);
      }
   }
}
