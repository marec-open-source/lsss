package no.imr.tools.jogl.node;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.glu.GLU;
import no.imr.tools.math.linalg.Ray;
import no.imr.tools.math.linalg.Vec3;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;

/**
 * Perspective projection.
 */
public final class JoglPerspectiveModule extends JoglModule {
   private float fovy = 45;
   private float near = 0.01f;
   private float far = 7;

   public JoglPerspectiveModule(@Nullable GLContext glContext) {
      super(glContext);
   }

   public float getFovy() {
      return fovy;
   }

   public void setFovy(float fovy) {
      this.fovy = fovy;
      triggerProjectionUpdate();
   }

   public float getNear() {
      return near;
   }

   public float getFar() {
      return far;
   }

   public void setNearFar(float near, float far) {
      this.near = near;
      this.far = far;
      triggerProjectionUpdate();
   }

   @Override
   void setProjection(GL2 gl) {
      float aspect = (float) getWidth() / getHeight();
      new GLU().gluPerspective(fovy, aspect, near, far);
   }

   private Vec3 pixPosToEyeDirection(Point2D pixPos) {
      float x = (float) (pixPos.getX() - 0.5 * getWidth());
      float y = (float) (0.5 * getHeight() - pixPos.getY());
      float z = (float) (0.5 * getHeight() / Math.tan(Math.toRadians(fovy / 2)));
      Vec3 dir = new Vec3(x, y, -z);
      return dir.unit();
   }

   @Override
   public Ray pixPosToViewRay(Point2D pixPos) {
      Vec3 origin = Vec3.ZERO;
      Vec3 rayDir = pixPosToEyeDirection(pixPos);
      return new Ray(origin, rayDir);
   }
}
