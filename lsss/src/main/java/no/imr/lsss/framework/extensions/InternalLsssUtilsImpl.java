package no.imr.lsss.framework.extensions;

import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.marec.lsss.api.internal.InternalLsssUtils;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

final class InternalLsssUtilsImpl implements InternalLsssUtils {
   static final InternalLsssUtilsImpl INSTANCE = new InternalLsssUtilsImpl();

   private InternalLsssUtilsImpl() {
   }

   @Override
   public void drawText(Graphics2D g, String text, int x, int y) {
      GuiText.draw(g, text, g.getColor(), x, y, GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.BASELINE, null);
   }

   @Override
   public boolean lineStripIntersectsRectangle(Path2D lineStrip, Rectangle2D rectangle) {
      return GuiUtils.intersects(lineStrip, rectangle);
   }
}
