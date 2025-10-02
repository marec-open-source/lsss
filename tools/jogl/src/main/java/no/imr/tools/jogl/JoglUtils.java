package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import no.imr.tools.math.linalg.Vec3;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static com.jogamp.opengl.GL2.*;

/**
 * General methods related to JOGL.
 */
public final class JoglUtils {
   private JoglUtils() {
   }

   public static boolean isJoglUsable() {
      try {
         GLProfile.getDefault();
         return true;
      } catch (GLException e) {
         return false;
      }
   }

   public static void glColor(GL2 gl, Color color) {
      gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
   }

   public static void glVertex(GL2 gl, Vec3 vertex) {
      gl.glVertex3f(vertex.x(), vertex.y(), vertex.z());
   }

   public static void glVertex(GL2 gl, Point2D point) {
      gl.glVertex2d(point.getX(), point.getY());
   }

   public static void glVertex(GL2 gl, Point2D point, Point2D referencePoint) {
      gl.glVertex2d(point.getX() - referencePoint.getX(), point.getY() - referencePoint.getY());
   }

   public static void drawPoints(GL2 gl, Collection<? extends Point2D> points) {
      gl.glBegin(GL_POINTS);
      points.forEach(p -> glVertex(gl, p));
      gl.glEnd();
   }

   public static void drawPoints(GL2 gl, Collection<? extends Point2D> points, Point2D referencePoint) {
      gl.glBegin(GL_POINTS);
      points.forEach(p -> glVertex(gl, p, referencePoint));
      gl.glEnd();
   }

   public static void drawPoints(GL2 gl, Stream<? extends Point2D> points, Point2D referencePoint) {
      gl.glBegin(GL_POINTS);
      points.forEach(p -> glVertex(gl, p, referencePoint));
      gl.glEnd();
   }

   public static void glVertexBox(GL2 gl, Rectangle2D box, Point2D referencePoint) {
      double x = box.getX() - referencePoint.getX();
      double y = box.getY() - referencePoint.getY();
      glVertexBox(gl, x, y, box.getWidth(), box.getHeight());
   }

   public static void glVertexBox(GL2 gl, Rectangle2D box) {
      glVertexBox(gl, box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight());
   }

   public static void glVertexBox(GL2 gl, double x, double y, double dx, double dy) {
      double x1 = x + dx;
      double y1 = y + dy;
      gl.glVertex2d(x, y);
      gl.glVertex2d(x1, y);
      gl.glVertex2d(x1, y1);
      gl.glVertex2d(x, y1);
   }

   public static void drawLine(GL2 gl, double x0, double y0, double x1, double y1) {
      gl.glBegin(GL_LINES);
      gl.glVertex2d(x0, y0);
      gl.glVertex2d(x1, y1);
      gl.glEnd();
   }

   /**
    * Draw a triangle and compute the normal vector.
    *
    * @param gl the gl object
    * @param p1 a point
    * @param p2 a point
    * @param p3 a point
    */
   public static void drawTriangle(GL2 gl, Vec3 p1, Vec3 p2, Vec3 p3) {
      gl.glBegin(GL_TRIANGLES);
      Vec3 normal = computeNormal(p1, p2, p3);
      gl.glNormal3f(normal.x(), normal.y(), normal.z());
      glVertex(gl, p1);
      glVertex(gl, p2);
      glVertex(gl, p3);
      gl.glEnd();
   }

   /**
    * Computes a normal to the input set of points.
    * Normalization is not performed since it will be destroyed by rescaling later.
    *
    * @param p1 p1
    * @param p2 p2
    * @param p3 p3
    * @return the normal vector
    */
   private static Vec3 computeNormal(Vec3 p1, Vec3 p2, Vec3 p3) {
      Vec3 v1 = p2.minus(p1);
      Vec3 v2 = p3.minus(p2);
      return v2.cross(v1);
   }

   /**
    * Sets up light in the scene, given a diffuse color, light position and material color.
    * Objects that are drawn after this is called is drawn with lightning.
    *
    * @param gl            gl object
    * @param diffuseColor  the color of the diffuse light
    * @param position      the position of the light source
    * @param materialColor the color of the material to be drawn
    */
   public static void enableLight(GL2 gl, float[] diffuseColor, float[] position, float[] materialColor) {
      gl.glEnable(GL_LIGHTING);
      gl.glShadeModel(GL_SMOOTH);

      gl.glLightfv(GL_LIGHT0, GL_DIFFUSE, diffuseColor, 0);

      gl.glLightfv(GL_LIGHT0, GL_POSITION, position, 0);

      gl.glMaterialfv(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, materialColor, 0);

      gl.glEnable(GL_LIGHT0);
      gl.glEnable(GL_NORMALIZE);
   }

   /**
    * Disables the light. Objects that are drawn after this is called does not get lighting.
    *
    * @param gl the gl object
    */
   public static void disableLight(GL2 gl) {
      gl.glDisable(GL_LIGHTING);
      gl.glDisable(GL_LIGHT0);
   }

   public static void renderCubicObject(GL2 gl, List<Vec3> corners) {
      gl.glBegin(GL_QUADS);

      glVertex(gl, corners.get(0));
      glVertex(gl, corners.get(1));
      glVertex(gl, corners.get(2));
      glVertex(gl, corners.get(3));

      glVertex(gl, corners.get(1));
      glVertex(gl, corners.get(5));
      glVertex(gl, corners.get(6));
      glVertex(gl, corners.get(2));

      glVertex(gl, corners.get(2));
      glVertex(gl, corners.get(6));
      glVertex(gl, corners.get(7));
      glVertex(gl, corners.get(3));

      glVertex(gl, corners.get(3));
      glVertex(gl, corners.get(7));
      glVertex(gl, corners.get(4));
      glVertex(gl, corners.get(0));

      glVertex(gl, corners.get(4));
      glVertex(gl, corners.get(7));
      glVertex(gl, corners.get(6));
      glVertex(gl, corners.get(5));

      glVertex(gl, corners.get(5));
      glVertex(gl, corners.get(1));
      glVertex(gl, corners.get(0));
      glVertex(gl, corners.get(4));

      gl.glEnd();
   }

   public static void renderCube(GL2 gl, Vec3 lowerLeftCorner, Vec3 extent) {
      Vec3 c0 = lowerLeftCorner;
      Vec3 c1 = lowerLeftCorner.plus(extent.x(), 0, 0);
      Vec3 c2 = lowerLeftCorner.plus(extent.x(), 0, extent.z());
      Vec3 c3 = lowerLeftCorner.plus(0, 0, extent.z());

      Vec3 c4 = lowerLeftCorner.plus(0, extent.y(), 0);
      Vec3 c5 = lowerLeftCorner.plus(extent.x(), extent.y(), 0);
      Vec3 c6 = lowerLeftCorner.plus(extent);
      Vec3 c7 = lowerLeftCorner.plus(0, extent.y(), extent.z());

      List<Vec3> corners = List.of(c0, c1, c2, c3, c4, c5, c6, c7);
      renderCubicObject(gl, corners);
   }

   public static void renderVertexList(GL2 gl, List<Vec3> vertices) {
      vertices.forEach(vertex -> glVertex(gl, vertex));
   }

   public static GLContext createSharableContext(String profile) {
      //Inspired by a jogl unit test: https://jogamp.org/git/?p=jogl.git;a=blob;f=src/test/com/jogamp/opengl/test/junit/jogl/acore/TestSharedContextListAWT.java
      //There might be better ways of sharing contexts between views.
      //See also https://forum.jogamp.org/Context-sharing-regression-from-JSR231-to-Jogl-2-x-td3332167.html
      //         https://forum.jogamp.org/Multiple-GLCanvas-FPSAnimator-Hang-td4030581.html
      GLProfile glProfile = GLProfile.get(profile);
      GLCapabilities capabilities = new GLCapabilities(glProfile);
      GLOffscreenAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(glProfile).createOffscreenAutoDrawable(null, capabilities, null, 256, 256);
      // init and render one frame
      sharedDrawable.display();
      return sharedDrawable.getContext();
   }
}
