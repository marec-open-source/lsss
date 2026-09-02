package no.imr.lsss.util.overlays;

import no.imr.lsss.modules.OverlayDisplayData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public record PingMarkerDisplayData(
      Rectangle2D.Float marker,
      Color color
) implements OverlayDisplayData {

   public PingMarkerDisplayData(float x, float markerWidth, float height, Color color) {
      this(new Rectangle2D.Float(x - markerWidth / 2, 0, markerWidth, height), color);
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
