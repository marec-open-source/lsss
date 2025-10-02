package no.imr.tools.swing.icons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;

/**
 * A constant colored icon.
 */
public final class ColorIcon extends AbstractIcon {
   private final Color color;

   public ColorIcon(Color color, int width, int height) {
      super(width, height);

      this.color = color;
   }

   @Override
   public void paintIcon(Component c, Graphics2D g, int x, int y) {
      g.setColor(color);
      g.fillRect(x, y, getIconWidth(), getIconHeight());
   }
}
