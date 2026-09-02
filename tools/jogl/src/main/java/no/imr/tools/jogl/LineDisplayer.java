package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import no.imr.tools.jogl.node.JoglDisplayNode;
import no.imr.tools.math.linalg.Vec3;

import java.util.List;

import static com.jogamp.opengl.GL2.*;

public final class LineDisplayer extends JoglDisplayNode {
   private List<Vec3> positions = List.of();
   private Vec3 color = new Vec3(0f, 0, 1);

   public LineDisplayer() {
   }

   public void setColor(Vec3 color) {
      this.color = color;
   }

   public void setPositions(List<Vec3> positions) {
      this.positions = positions;
   }

   public void clear() {
      positions = List.of();
   }

   @Override
   protected void draw(GL2 gl) {
      if (positions.isEmpty()) {
         return;
      }

      gl.glColor3f(color.x(), color.y(), color.z());
      gl.glLineWidth(3 * getUiScaleFactor());

      gl.glBegin(GL_LINE_STRIP);
      JoglUtils.glVertices(gl, positions);
      gl.glEnd();
   }
}
