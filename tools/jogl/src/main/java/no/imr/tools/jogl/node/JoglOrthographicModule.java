package no.imr.tools.jogl.node;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import no.imr.tools.math.linalg.Ray;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;

/**
 * Orthographic projection.
 */
public sealed class JoglOrthographicModule extends JoglModule permits Jogl2dModule {
   private ClippingPlanes clippingPlanes = new ClippingPlanes(FloatRange.of(-1, 1), FloatRange.of(-1, 1));
   private ClippingPlanes adjustedClippingPlanes = clippingPlanes;
   private final FloatRange zRange;

   public JoglOrthographicModule(@Nullable GLContext glContext, FloatRange zRange) {
      super(glContext);

      this.zRange = zRange;
   }

   public ClippingPlanes getClippingPlanes() {
      return adjustedClippingPlanes;
   }

   public void setClippingPlanes(FloatRange x, FloatRange y) {
      clippingPlanes = new ClippingPlanes(x, y);
      triggerProjectionUpdate();
   }

   @Override
   void setProjection(GL2 gl) {
      adjustedClippingPlanes = clippingPlanes.adjust(getGlWidth(), getGlHeight());
      gl.glOrtho(adjustedClippingPlanes.x().min(), adjustedClippingPlanes.x().max(),
            adjustedClippingPlanes.y().min(), adjustedClippingPlanes.y().max(),
            zRange.min(), zRange.max());
   }

   @Override
   public Ray pixPosToViewRay(Point2D pixPos) {
      Vec3 origin = new Vec3(
            adjustedClippingPlanes.x().fractionToValue((float) pixPos.getX() / getJava2dWidth()),
            adjustedClippingPlanes.y().fractionToValue(1 - (float) pixPos.getY() / getJava2dHeight()),
            zRange.max());
      Vec3 dir = new Vec3(0, 0, -1);
      return new Ray(origin, dir);
   }

   public float getModelWidth() {
      return adjustedClippingPlanes.x().getSize();
   }

   public Point2D pixPosToWorldPos(Point2D pixPos) {
      float x = adjustedClippingPlanes.x().fractionToValue((float) pixPos.getX() / getJava2dWidth());
      float y = adjustedClippingPlanes.y().fractionToValue(1 - (float) pixPos.getY() / getJava2dHeight());
      return new Point2D.Float(x, y);
   }

   public Point2D worldPosToGlPos(Point2D worldPos) {
      float x = getGlWidth() * adjustedClippingPlanes.x().valueToFraction((float) worldPos.getX());
      float y = getGlHeight() * adjustedClippingPlanes.y().valueToFraction((float) worldPos.getY());
      return new Point2D.Float(x, y);
   }

   public float getWorldCoordinatesPerGlPixel() {
      return getModelWidth() / getGlWidth();
   }

   public float getWorldCoordinatesPerJava2dPixel() {
      return getModelWidth() / getJava2dWidth();
   }
}
