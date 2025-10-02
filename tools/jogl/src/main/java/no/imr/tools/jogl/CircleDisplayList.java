package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;

import java.util.List;

import static com.jogamp.opengl.GL2.*;

public final class CircleDisplayList {
   private final DisplayList displayList = new DisplayList(this::drawCircle);
   private int segmentCount;

   public CircleDisplayList(int segmentCount) {
      this.segmentCount = segmentCount;
   }

   public void init(GL2 gl, List<JoglDisposable> disposables) {
      displayList.init(gl, disposables);
   }

   public void update(int aSegmentCount) {
      if (segmentCount == aSegmentCount) {
         return;
      }
      segmentCount = aSegmentCount;
      displayList.triggerUpdate();
   }

   public void draw(GL2 gl) {
      displayList.draw(gl);
   }

   private void drawCircle(GL2 gl) {
      gl.glBegin(GL_LINE_LOOP);
      double deltaAngle = 2 * Math.PI / segmentCount;
      for (int i = 0; i < segmentCount; i++) {
         double angle = i * deltaAngle;
         float x = (float) Math.cos(angle);
         float y = (float) Math.sin(angle);
         gl.glVertex2f(x, y);
      }
      gl.glEnd();
   }
}
