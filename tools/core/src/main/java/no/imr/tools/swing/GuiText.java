package no.imr.tools.swing;

import org.jspecify.annotations.Nullable;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

public final class GuiText {
   private final String text;
   private final Color color;
   private final float x;
   private final float y;
   private final HorizontalAlignment horizontalAlignment;
   private final VerticalAlignment verticalAlignment;
   private final @Nullable Rectangle bounds;

   public GuiText(String text, Color color, float x, float y,
                  HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment,
                  @Nullable Rectangle bounds) {
      this.text = text;
      this.color = color;
      this.x = x;
      this.y = y;
      this.horizontalAlignment = horizontalAlignment;
      this.verticalAlignment = verticalAlignment;
      this.bounds = bounds;
   }

   public void draw(Graphics2D g2d) {
      draw(g2d, text, color, x, y, horizontalAlignment, verticalAlignment, bounds);
   }

   public static void draw(Graphics2D g2d, String text, Color color, float x, float y,
                           HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment,
                           @Nullable Rectangle bounds) {
      if (text.isEmpty()) {
         // TextLayout throws on empty text.
         return;
      }
      TextLayout textLayout = newTextLayout(g2d, text);
      float ascent = textLayout.getAscent();
      float descent = textLayout.getDescent();
      float width = textLayout.getAdvance();
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
         // Keep text inside bounds, but never push it past left or upper edge.
         x = Math.min(x, bounds.x + bounds.width - width);
         x = Math.max(x, bounds.x);
         y = Math.min(y, bounds.y + bounds.height - descent);
         y = Math.max(y, bounds.y + ascent);
      }

      // Draw semi-transparent background.
      Color backgroundColor = ColorUtils.contrastingBlackOrWhite(color);
      g2d.setColor(backgroundColor);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
      g2d.fill(new Rectangle2D.Float(x - 2, y - ascent, width + 4, height));

      // Draw text.
      g2d.setColor(color);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
      textLayout.draw(g2d, x, y);
   }

   public static void draw(Graphics2D g2d, String text, float x, float y) {
      if (text.isEmpty()) {
         // TextLayout throws on empty text.
         return;
      }
      newTextLayout(g2d, text).draw(g2d, x, y);
   }

   private static TextLayout newTextLayout(Graphics2D g2d, String text) {
      FontRenderContext fontRenderContext = new FontRenderContext(null, true, g2d.getFontRenderContext().usesFractionalMetrics());
      return new TextLayout(text, g2d.getFont(), fontRenderContext);
   }

   public enum HorizontalAlignment {
      LEFT, CENTER, RIGHT
   }

   public enum VerticalAlignment {
      BASELINE, BOTTOM, CENTER, TOP
   }
}
