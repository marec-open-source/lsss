package no.imr.tools.swing.icons;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Helper base class for icons.
 */
public abstract class AbstractIcon implements Icon {
   private final int width;
   private final int height;

   protected AbstractIcon(int width, int height) {
      this.width = width;
      this.height = height;
   }

   @Override
   public final void paintIcon(Component c, Graphics g, int x, int y) {
      paintIcon(c, (Graphics2D) g, x, y);
   }

   protected abstract void paintIcon(Component c, Graphics2D g, int x, int y);

   @Override
   public int getIconWidth() {
      return width;
   }

   @Override
   public int getIconHeight() {
      return height;
   }
}
