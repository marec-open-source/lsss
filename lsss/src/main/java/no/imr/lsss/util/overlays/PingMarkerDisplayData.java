package no.imr.lsss.util.overlays;

import no.imr.lsss.modules.OverlayDisplayData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public final class PingMarkerDisplayData extends OverlayDisplayData {
   private final Rectangle2D.Float marker;
   private final Color color;

   public PingMarkerDisplayData(float x, float markerWidth, float height, Color color) {
      marker = new Rectangle2D.Float(x - markerWidth / 2, 0, markerWidth, height);
      this.color = color;
   }

   @Override
   public void draw(Graphics2D g2d) {
      g2d.setColor(color);
      g2d.fill(marker);
   }

   @Override
   public boolean intersects(Rectangle2D rectangle) {
      return marker.intersects(rectangle);
   }
}
