package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.OverlayDisplayData;
import no.marec.lsss.api.modules.LsssOverlayDisplayData;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

record ExtensionOverlayDisplayData(LsssOverlayDisplayData data) implements OverlayDisplayData {
   @Override
   public void draw(Graphics2D g2d) {
      data.draw(g2d);
   }

   @Override
   public void drawText(Graphics2D g2d) {
      data.drawText(g2d);
   }

   @Override
   public boolean intersects(Rectangle2D rectangle) {
      return data.intersects(rectangle);
   }
}
