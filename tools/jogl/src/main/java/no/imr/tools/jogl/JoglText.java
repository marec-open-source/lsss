package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.gl2.GLUT;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.jogamp.opengl.GL2.*;

public final class JoglText {
   private final GLUT glut;
   private final double coordinatesPerPixel;
   private final int font;
   private final double margin;
   private final double dy;
   private final List<Text> texts = new ArrayList<>();

   public JoglText(GLUT glut, double coordinatesPerPixel, int font, int textHeight) {
      this.glut = glut;
      this.coordinatesPerPixel = coordinatesPerPixel;
      this.font = font;
      margin = 2 * coordinatesPerPixel;
      dy = textHeight * coordinatesPerPixel + 2 * margin;
   }

   public void add(String text, double x, HorizontalAlignment horizontalAlignment, double y, VerticalAlignment verticalAlignment, Color color) {
      int textLength = glut.glutBitmapLength(font, text);
      double dx = textLength * coordinatesPerPixel + 2 * margin;
      texts.add(new Text(text, horizontalAlignment.adjust(x, dx), verticalAlignment.adjust(y, dy), dx, color));
   }

   public void draw(GL2 gl) {
      gl.glColor4f(0, 0, 0, 0.5f);
      gl.glEnable(GL_BLEND);
      gl.glBegin(GL_QUADS);
      texts.forEach(text -> JoglUtils.glVertexBox(gl, text.x, text.y, text.dx, dy));
      gl.glEnd();
      gl.glDisable(GL_BLEND);

      texts.forEach(text -> {
         JoglUtils.glColor(gl, text.color);
         gl.glRasterPos2d(text.x + margin, text.y + 2 * margin); // Text seems to extend below baseline
         glut.glutBitmapString(font, text.text);
      });
   }

   private record Text(String text, double x, double y, double dx, Color color) {
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
