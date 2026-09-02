package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import static com.jogamp.opengl.GL2.*;

public final class JoglText {
   private final double coordinatesPerGlPixel;
   private final double margin;
   private final double boxHeight;
   private final List<Text> texts = new ArrayList<>();
   private final TextRenderer textRenderer;

   public JoglText(double coordinatesPerGlPixel, Font font, float uiScaleFactor) {
      this.coordinatesPerGlPixel = coordinatesPerGlPixel;
      margin = 2 * coordinatesPerGlPixel * uiScaleFactor;
      font = font.deriveFont(font.getSize() * uiScaleFactor);
      boxHeight = font.getSize() * coordinatesPerGlPixel + 2 * margin;
      textRenderer = new TextRenderer(font, true, false);
   }

   public void dispose() {
      textRenderer.dispose();
   }

   public void add(String text, double x, HorizontalAlignment horizontalAlignment, double y, VerticalAlignment verticalAlignment, Color color) {
      if (text.isEmpty()) {
         return;
      }
      double dx = getBoxWidth(text);
      texts.add(new Text(text, horizontalAlignment.adjust(x, dx), verticalAlignment.adjust(y, boxHeight), dx, color));
   }

   public double getBoxHeight() {
      return boxHeight;
   }

   public double getBoxWidth(String text) {
      return textRenderer.getBounds(text).getWidth() * coordinatesPerGlPixel + 2 * margin;
   }

   public void draw(GL2 gl) {
      gl.glColor4f(0, 0, 0, 0.5f);
      gl.glEnable(GL_BLEND);
      gl.glBegin(GL_QUADS);
      for (Text text : texts) {
         JoglUtils.glVertexBox(gl, text.x, text.y, text.boxWidth, boxHeight);
      }
      gl.glEnd();
      gl.glDisable(GL_BLEND);

      textRenderer.begin3DRendering();
      for (Text text : texts) {
         textRenderer.setColor(text.color);
         textRenderer.draw3D(text.text, (float) (text.x + margin), (float) (text.y + 2 * margin), 0, (float) coordinatesPerGlPixel);
      }
      textRenderer.end3DRendering();
   }

   private record Text(String text, double x, double y, double boxWidth, Color color) {
   }

   public enum HorizontalAlignment {
      LEFT, CENTER, RIGHT;

      private double adjust(double x, double dx) {
         return switch (this) {
            case LEFT -> x;
            case CENTER -> x - dx / 2;
            case RIGHT -> x - dx;
         };
      }
   }

   public enum VerticalAlignment {
      BOTTOM, CENTER, TOP;

      private double adjust(double y, double dy) {
         return switch (this) {
            case BOTTOM -> y;
            case CENTER -> y - dy / 2;
            case TOP -> y - dy;
         };
      }
   }
}
