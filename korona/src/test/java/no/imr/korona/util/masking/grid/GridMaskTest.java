package no.imr.korona.util.masking.grid;

import no.imr.korona.util.masking.grid.surfaces.ShapeLimitedSurface;
import no.imr.korona.util.masking.grid.surfaces.Surface;
import no.imr.tools.math.Function2D;
import no.imr.tools.math.GeometryUtils;
import no.imr.tools.math.linalg.Matrix3;
import no.imr.tools.math.linalg.TRS;
import no.imr.tools.math.linalg.Transform;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.Test;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class GridMaskTest {
   private final Transform transform0 = Transform.identity();
   private final Transform transform1 = new TRS(new Vec3(0.1f, 4, 1234), Matrix3.createRotation(23, new Vec3(1, 6, 5).unit()));
   private final Transform transform2 = new TRS(new Vec3(0, -3, 0.001f), Matrix3.createRotation(-137, new Vec3(2, 0.1f, -1).unit()));

   @Test
   void sphere() {
      GridMask gridMask = new GridMask(0.01);
      gridMask.add(List.of(new Hemisphere(1, true), new Hemisphere(1, false)));

      assertEquals(sphereVolume(1), gridMask.getVolume(), 1e-3);
      assertEquals(circleArea(1), gridMask.getColumnCount() * gridMask.getDeltaX() * gridMask.getDeltaY(), 1e-2);
   }

   @Test
   void sphereShapeLimitedSurface() {
      GridMask gridMask = new GridMask(0.01);
      Shape shape = new Ellipse2D.Double(-1, -1, 2, 2);
      Vec3 center = Vec3.ZERO;
      ShapeLimitedSurface up = new ShapeLimitedSurface(shape, Function2D.sphere(center, 1, true));
      ShapeLimitedSurface down = new ShapeLimitedSurface(shape, Function2D.sphere(center, 1, false));
      gridMask.add(List.of(up, down));
      assertEquals(sphereVolume(1), gridMask.getVolume(), 1e-3);
   }

   @Test
   void box() {
      checkBox(transform0, 1e-15);
      checkBox(transform1, 1e-4);
      checkBox(transform2, 1e-4);
   }

   private static void checkBox(Transform transform, double delta) {
      Vec3[] p = createBoxPoints();
      GridMask gridMask = new GridMask(0.01);
      gridMask.add(GridMaskUtils.createBox(transform, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]));
      assertEquals(1, gridMask.getVolume(), delta);
   }

   @Test
   void pyramid() {
      checkPyramid(FloatRange.of(0.5f, 1.0f), FloatRange.of(1.0f, 1.4f), FloatRange.of(0.5f, 1.0f), transform0);
      checkPyramid(FloatRange.of(1.1f, 1.5f), FloatRange.of(-0.1f, 0.3f), FloatRange.of(1.5f, 2.0f), transform1);
      checkPyramid(FloatRange.of(1.1f, 2.0f), FloatRange.of(2.0f, 3.0f), FloatRange.of(2.0f, 2.1f), transform2);
   }

   private static void checkPyramid(FloatRange radius, FloatRange theta, FloatRange phi, Transform transform) {
      GridMask gridMask = new GridMask(0.01);
      gridMask.add(GridMaskUtils.createPyramidSegment(transform, radius, theta, phi));
      assertEquals(pyramidSegmentVolume(radius, theta, phi), gridMask.getVolume(), 1e-3);
   }

   @Test
   void addGridMask() {
      GridMask gridMask1 = new GridMask(1);
      gridMask1.add(createSurfaces(new Rectangle2D.Double(0, 0, 2, 2), Function2D.constant(0), Function2D.constant(2)));
      GridMask gridMask2 = new GridMask(1);
      gridMask2.add(createSurfaces(new Rectangle2D.Double(1, 1, 2, 2), Function2D.constant(1), Function2D.constant(3)));
      gridMask1.add(gridMask2);
      assertEquals(15, gridMask1.getVolume());
   }

   @Test
   void save() throws IOException {
      GridMask gridMask = new GridMask(1);
      gridMask.add(createSurfaces(new Rectangle2D.Double(0, 0, 2, 2), Function2D.constant(0), Function2D.constant(2)));
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      gridMask.save(new DataOutputStream(byteArrayOutputStream));
      GridMask gridMask2 = new GridMask(new DataInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
      assertEquals(gridMask, gridMask2);
   }

   private static double circleArea(double r) {
      return Math.PI * r * r;
   }

   private static double sphereVolume(double r) {
      return (4.0 / 3.0) * Math.PI * r * r * r;
   }

   private static double pyramidSegmentVolume(FloatRange r, FloatRange theta, FloatRange phi) {
      return pyramidVolume(r.max(), theta, phi) - pyramidVolume(r.min(), theta, phi);
   }

   private static double pyramidVolume(double r, FloatRange theta, FloatRange phi) {
      Vec3 a = GeometryUtils.sphericalToCartesian(r, theta.min(), phi.min());
      Vec3 b = GeometryUtils.sphericalToCartesian(r, theta.min(), phi.max());
      Vec3 c = GeometryUtils.sphericalToCartesian(r, theta.max(), phi.max());
      Vec3 d = GeometryUtils.sphericalToCartesian(r, theta.max(), phi.min());
      double h = a.plus(c).length() / 2;
      return area(a, b, c, d) * h / 3;
   }

   private static double area(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
      double a1 = b.minus(a).cross(d.minus(a)).length();
      double a2 = b.minus(c).cross(d.minus(c)).length();
      return (a1 + a2) / 2;
   }

   private static Vec3[] createBoxPoints() {
      return new Vec3[]{
            Vec3.ZERO,
            new Vec3(0, 0, 1),
            new Vec3(0, 1, 0),
            new Vec3(0, 1, 1),
            new Vec3(1, 0, 0),
            new Vec3(1, 0, 1),
            new Vec3(1, 1, 0),
            new Vec3(1, 1, 1)
      };
   }

   private static List<Surface> createSurfaces(Shape shape, Function2D f1, Function2D f2) {
      return List.of(
            new ShapeLimitedSurface(shape, f1),
            new ShapeLimitedSurface(shape, f2)
      );
   }
}
