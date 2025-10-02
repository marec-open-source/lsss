package no.imr.tools.swing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

public final class PaintFactory {
   private PaintFactory() {
   }

   public static Paint createCrossedPaint(Color color, int textureSize, int crossSize, int xOffset) {
      BufferedImage tile = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);

      int margin = (textureSize - crossSize) / 2;

      int contrastingRGB = ColorUtils.contrastingColorWithSameHue(color, 3).getRGB();

      Graphics2D g = tile.createGraphics();
      g.setColor(color);
      g.fillRect(0, 0, textureSize, textureSize);
      g.dispose();

      for (int i = 0; i < crossSize; i++) {
         tile.setRGB((margin + i + xOffset) % textureSize, textureSize / 2, contrastingRGB);
         tile.setRGB((textureSize / 2 + xOffset) % textureSize, margin + i, contrastingRGB);
      }

      return new TexturePaint(tile, new Rectangle(0, 0, textureSize, textureSize));
   }
}
