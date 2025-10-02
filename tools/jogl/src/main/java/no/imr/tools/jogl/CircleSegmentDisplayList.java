package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;

import java.util.List;

import static com.jogamp.opengl.GL2.*;

public final class CircleSegmentDisplayList {
   private final DisplayList displayList = new DisplayList(this::drawCircle);
   private int segmentCount = 1;
   private double beginBeamAngleDeg;
   private double endBeamAngleDeg;

   public CircleSegmentDisplayList() {
   }

   public void init(GL2 gl, List<JoglDisposable> disposables) {
      displayList.init(gl, disposables);
   }

   public void update(int aSegmentCount, double aBeginBeamAngleDeg, double aEndBeamAngleDeg) {
      if (segmentCount == aSegmentCount && beginBeamAngleDeg == aBeginBeamAngleDeg && endBeamAngleDeg == aEndBeamAngleDeg) {
         return;
      }
      segmentCount = aSegmentCount;
      beginBeamAngleDeg = aBeginBeamAngleDeg;
      endBeamAngleDeg = aEndBeamAngleDeg;
      displayList.triggerUpdate();
   }

   public void draw(GL2 gl) {
      displayList.draw(gl);
   }

   private void drawCircle(GL2 gl) {
      double beginAngle = Math.toRadians(beginBeamAngleDeg);
      double deltaAngle = Math.toRadians(endBeamAngleDeg - beginBeamAngleDeg) / segmentCount;

      gl.glBegin(GL_LINE_STRIP);
      for (int i = 0; i <= segmentCount; i++) {
         double angle = beginAngle + i * deltaAngle;
         float x = (float) Math.cos(angle);
         float y = (float) Math.sin(angle);
         gl.glVertex2f(x, y);
      }
      gl.glEnd();
   }
}
