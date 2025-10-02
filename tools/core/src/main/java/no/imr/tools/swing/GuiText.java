package no.imr.tools.swing;

import org.jspecify.annotations.Nullable;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

public final class GuiText implements Drawable {
   private final String text;
   private final Color color;
   private final float x;
   private final float y;
   private final HorizontalAlignment horizontalAlignment;
   private final VerticalAlignment verticalAlignment;
   private final @Nullable Rectangle bounds;

   public GuiText(String text, Color color, float x, float y,
                  HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, @Nullable Rectangle bounds) {
      this.text = text;
      this.color = color;
      this.x = x;
      this.y = y;
      this.horizontalAlignment = horizontalAlignment;
      this.verticalAlignment = verticalAlignment;
      this.bounds = bounds;
   }

   @Override
   public void draw(Graphics2D g2d) {
      draw(g2d, text, color, x, y, horizontalAlignment, verticalAlignment, bounds);
   }

   public static void draw(Graphics2D g2d, String text, Color color, float x, float y,
                           HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, @Nullable Rectangle bounds) {
      if (text.isEmpty()) {
         // TextLayout throws on empty text.
         return;
      }
      FontMetrics fontMetrics = g2d.getFontMetrics();
      LineMetrics lineMetrics = fontMetrics.getLineMetrics(text, g2d);
      float ascent = lineMetrics.getAscent();
      float descent = lineMetrics.getDescent();
      float width = fontMetrics.stringWidth(text);
      float height = ascent + descent;

      x += switch (horizontalAlignment) {
         case LEFT -> 0;
         case CENTER -> -width / 2;
         case RIGHT -> -width;
      };

      y += switch (verticalAlignment) {
         case BASELINE -> 0;
         case BOTTOM -> -descent;
         case CENTER -> (ascent - descent) / 2;
         case TOP -> ascent;
      };

      if (bounds != null) {
         if (x < bounds.x) {
            x = bounds.x;
         } else if (x > bounds.x + bounds.width - width) {
            x = bounds.x + bounds.width - width;
         }

         if (y < bounds.y + ascent) {
            y = bounds.y + ascent;
         } else if (y > bounds.y + bounds.height - descent) {
            y = bounds.y + bounds.height - descent;
         }
      }

      // Draw semi-transparent background
      Color backgroundColor = ColorUtils.contrastingBlackOrWhite(color);
      g2d.setColor(backgroundColor);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
      g2d.fill(new Rectangle2D.Float(x - 1, y - ascent, width + 2, height));

      // Draw text
      g2d.setColor(color);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
      draw(g2d, text, x, y);
   }

   public static void draw(Graphics2D g2d, String text, float x, float y) {
      if (text.isEmpty()) {
         // TextLayout throws on empty text.
         return;
      }
      FontRenderContext fontRenderContext = new FontRenderContext(null, true, g2d.getFontRenderContext().usesFractionalMetrics());
      new TextLayout(text, g2d.getFont(), fontRenderContext).draw(g2d, x, y);
   }

   public enum HorizontalAlignment {
      LEFT, CENTER, RIGHT
   }

   public enum VerticalAlignment {
      BASELINE, BOTTOM, CENTER, TOP
   }
}
