package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Draws marks where pings are loaded.
 */
public final class PingMarkerOverlay extends BaseEchogramOverlay {
   private static final int MARKER_HEIGHT = 10;

   public PingMarkerOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getEchogramModule().echogramArea(),
            getInterpretationSettings().getPingSampler().getNewPingsChangeManager()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      List<Ping> availablePings = getInterpretationSettings().getPingSampler().getAvailablePings();
      if (availablePings.isEmpty()) {
         return null;
      }
      float y1 = getHeight();
      float y0 = y1 - MARKER_HEIGHT;
      Path2D.Float path = new Path2D.Float();
      EchogramPingSettings pingSettings = getPingSettings();
      int x0 = pingSettings.pingIndexToXIndex(availablePings.getFirst().getPingIndex());
      int x1 = x0 + 1;
      for (int i = 1; i < availablePings.size(); i++) {
         Ping ping = availablePings.get(i);
         int x = pingSettings.pingIndexToXIndex(ping.getPingIndex());
         if (x > x1) {
            GuiUtils.appendRectangle(path, x0, y0, x1, y1);
            x0 = x;
         }
         x1 = x + 1;
      }
      GuiUtils.appendRectangle(path, x0, y0, x1, y1);
      return new DisplayData(path);
   }

   private static final class DisplayData extends OverlayDisplayData {
      private final Path2D.Float path;

      private DisplayData(Path2D.Float path) {
         this.path = path;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.GREEN);
         g2d.fill(path);
      }
   }
}
