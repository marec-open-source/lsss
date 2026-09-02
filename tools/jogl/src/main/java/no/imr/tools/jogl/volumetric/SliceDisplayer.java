package no.imr.tools.jogl.volumetric;

import com.jogamp.opengl.GL2;
import no.imr.tools.math.linalg.LinalgUtils;
import no.imr.tools.math.linalg.Ray;
import no.imr.tools.math.linalg.Vec3;

import java.awt.Color;
import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL2.*;

public final class SliceDisplayer extends VolumeDisplayer {
   private Vec3 normal = new Vec3(0, 0, 1);
   private Vec3 intersectionPoint = Vec3.ZERO; //for debug
   private Vec3 sliceModelPos = Vec3.ZERO;
   private static final Color INTERSECTION_POINT_COLOR = new Color(0x333380);
   private static final float INTERSECTION_POINT_SIZE = 4;
   private static final float VALUE = 1.0f;
   private static final Vec3[] COORDS = {
         new Vec3(-VALUE, -VALUE, -VALUE), new Vec3(VALUE, -VALUE, -VALUE), new Vec3(VALUE, -VALUE, VALUE), new Vec3(-VALUE, -VALUE, VALUE),
         new Vec3(-VALUE, VALUE, -VALUE), new Vec3(VALUE, VALUE, -VALUE), new Vec3(VALUE, VALUE, VALUE), new Vec3(-VALUE, VALUE, VALUE),
   };

   public SliceDisplayer(String vertexShaderSource, String fragmentShaderSource) {
      super(vertexShaderSource, fragmentShaderSource);
      setOpaque(true);
   }

   @Override
   protected void doDisplay(GL2 gl) {
      Vec3 nocPos = mapModelToNoc(sliceModelPos);
      float[] param = computeCornerParams(COORDS, nocPos, normal);

      MarchingCube mc = new MarchingCube(0.0f, COORDS, param);

      bindTextures(gl);

      drawTexturedPolygons(mc.getPolys(), gl);
      releaseTextures(gl);

      gl.glColor3fv(FloatBuffer.wrap(INTERSECTION_POINT_COLOR.getColorComponents(null)));
      gl.glPushMatrix();
      Vec3 intersectionPoint = mapModelToNoc(this.intersectionPoint);
      drawPoint(gl, intersectionPoint, INTERSECTION_POINT_SIZE * getUiScaleFactor());

      gl.glPopMatrix();
   }

   private static void drawPoint(GL2 gl, Vec3 pos, float pointSize) {
      gl.glPointSize(pointSize);
      gl.glDisable(GL_DEPTH_TEST);
      gl.glBegin(GL_POINTS);
      gl.glVertex3f(pos.x(), pos.y(), pos.z());
      gl.glEnd();
      gl.glEnable(GL_DEPTH_TEST);
   }

   public Vec3 getPos() {
      return mapModelToNoc(sliceModelPos);
   }

   public Vec3 getSliceModelPos() {
      return sliceModelPos;
   }

   public Vec3 getNormal() {
      return normal;
   }

   public Vec3 getIntersectionPoint() {
      return intersectionPoint;
   }

   public static Color getIntersectionPointColor() {
      return INTERSECTION_POINT_COLOR;
   }

   public void modifyPos(int clicks) {
      Vec3 nocPos = mapModelToNoc(sliceModelPos);
      nocPos = nocPos.plus(normal.times(0.01f * clicks));
      sliceModelPos = mapNocToModel(nocPos);
   }

   public void setNormal(Vec3 normal) {
      this.normal = normal;
   }

   public void setIntersectionPoint(Vec3 intersectionPoint) {
      this.intersectionPoint = intersectionPoint;
   }

   public void moveSlicePosToIntersectionPoint() {
      sliceModelPos = intersectionPoint;
   }

   public Vec3 eyePos() {
      Vec3 nocPos = mapModelToNoc(sliceModelPos);
      return modelToEye(mapNocToModel(nocPos));
   }

   public Vec3 eyeNormal() {
      Vec3 nocPos = mapModelToNoc(sliceModelPos);
      Vec3 pos = modelToEye(mapNocToModel(nocPos));
      Vec3 normalEnd = modelToEye(mapNocToModel(nocPos.plus(normal)));
      Vec3 normal = normalEnd.minus(pos).unit();
      return normal;
   }

   public Vec3 computeIntersection(Ray viewRay) {
      Vec3 intersectionPoint = LinalgUtils.planeIntersection(viewRay, eyePos(), eyeNormal());
      if (intersectionPoint != null) {
         return eyeToModel(intersectionPoint);
      }
      return new Vec3(-1, -1, -1);
   }

   public void resetTransformations() {
      sliceModelPos = mapNocToModel(Vec3.ZERO);
      normal = new Vec3(0, 0, 1);
   }
}
