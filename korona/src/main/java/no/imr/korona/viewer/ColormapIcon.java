package no.imr.korona.viewer;

import no.imr.korona.color.Colormap;
import no.imr.tools.swing.icons.AbstractIcon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public final class ColormapIcon extends AbstractIcon {
   private final Colormap colormap;

   public ColormapIcon(Colormap colormap) {
      super(16, 16);

      this.colormap = colormap;
   }

   @Override
   protected void paintIcon(Component c, Graphics2D g, int x, int y) {
      int n = Math.max(1, (int) Math.round(getIconHeight() * g.getTransform().getScaleY()));
      float deltaValue = 1f / n;
      float deltaY = (float) getIconHeight() / n;
      Rectangle2D.Float rectangle = new Rectangle2D.Float(x + 1, 0, getIconWidth() - 2, deltaY);
      for (int i = 0; i < n; i++) {
         g.setColor(new Color(colormap.getRGB(1 - (i + 1) * deltaValue)));
         rectangle.y = y + i * deltaY;
         g.fill(rectangle);
      }
   }
}
