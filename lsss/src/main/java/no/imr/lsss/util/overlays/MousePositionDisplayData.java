package no.imr.lsss.util.overlays;

import no.imr.lsss.modules.OverlayDisplayData;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public final class MousePositionDisplayData extends OverlayDisplayData {
   private final Rectangle2D.Float xMarker;
   private final Ellipse2D.@Nullable Float depthMarker;

   public MousePositionDisplayData(float x, @Nullable Float y, int height) {
      xMarker = new Rectangle2D.Float(x - 1, 0, 2, height);
      depthMarker = y != null ? new Ellipse2D.Float(x - 5, y - 5, 10, 10) : null;
   }

   @Override
   public void draw(Graphics2D g2d) {
      g2d.setColor(Color.RED);
      g2d.fill(xMarker);
      if (depthMarker != null) {
         g2d.fill(depthMarker);
      }
   }
}
