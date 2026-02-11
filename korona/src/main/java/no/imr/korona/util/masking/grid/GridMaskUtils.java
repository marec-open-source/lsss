package no.imr.korona.util.masking.grid;

import no.imr.korona.util.masking.grid.surfaces.ShapeLimitedSurface;
import no.imr.korona.util.masking.grid.surfaces.Surface;
import no.imr.tools.math.Function2D;
import no.imr.tools.math.GeometryUtils;
import no.imr.tools.math.linalg.Transform;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class GridMaskUtils {
   private GridMaskUtils() {
   }

   public static List<Surface> createPyramidSegment(Transform transform, FloatRange radius, FloatRange theta, FloatRange phi) {
      return createBox(
            transform,
            GeometryUtils.sphericalToCartesian(radius.min(), theta.min(), phi.min()),
            GeometryUtils.sphericalToCartesian(radius.min(), theta.min(), phi.max()),
            GeometryUtils.sphericalToCartesian(radius.min(), theta.max(), phi.min()),
            GeometryUtils.sphericalToCartesian(radius.min(), theta.max(), phi.max()),
            GeometryUtils.sphericalToCartesian(radius.max(), theta.min(), phi.min()),
            GeometryUtils.sphericalToCartesian(radius.max(), theta.min(), phi.max()),
            GeometryUtils.sphericalToCartesian(radius.max(), theta.max(), phi.min()),
            GeometryUtils.sphericalToCartesian(radius.max(), theta.max(), phi.max())
      );
   }

   public static List<Surface> createBox(Transform transform, Vec3 aaa, Vec3 aab, Vec3 aba, Vec3 abb, Vec3 baa, Vec3 bab, Vec3 bba, Vec3 bbb) {
      return createBox(
            transform.transformPoint(aaa),
            transform.transformPoint(aab),
            transform.transformPoint(aba),
            transform.transformPoint(abb),
            transform.transformPoint(baa),
            transform.transformPoint(bab),
            transform.transformPoint(bba),
            transform.transformPoint(bbb)
      );
   }

   public static List<Surface> createBox(Vec3 aaa, Vec3 aab, Vec3 aba, Vec3 abb, Vec3 baa, Vec3 bab, Vec3 bba, Vec3 bbb) {
      List<Surface> surfaces = new ArrayList<>(6);

      addIfNonNull(surfaces, createPlane(aaa, aba, bba, baa));
      addIfNonNull(surfaces, createPlane(aab, abb, bbb, bab));

      addIfNonNull(surfaces, createPlane(aaa, baa, bab, aab));
      addIfNonNull(surfaces, createPlane(aba, bba, bbb, abb));

      addIfNonNull(surfaces, createPlane(aaa, aab, abb, aba));
      addIfNonNull(surfaces, createPlane(baa, bab, bbb, bba));

      return surfaces;
   }

   private static <T> void addIfNonNull(Collection<T> collection, @Nullable T item) {
      if (item != null) {
         collection.add(item);
      }
   }

   public static Path2D createPath(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
      Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, 5);
      path.moveTo(a.x(), a.y());
      path.lineTo(b.x(), b.y());
      path.lineTo(c.x(), c.y());
      path.lineTo(d.x(), d.y());
      path.closePath();
      return path;
   }

   public static @Nullable Surface createPlane(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
      Vec3 n = c.minus(a).cross(d.minus(b));
      float nz = n.z();
      if (nz == 0) {
         return null;
      }

      double dzDx = -n.x() / nz;
      double dzDy = -n.y() / nz;

      return new ShapeLimitedSurface(createPath(a, b, c, d), Function2D.linear(a, dzDx, dzDy));
   }
}
