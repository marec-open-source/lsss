package no.imr.deepvision.lsss.engine.mapping;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.geo.Earth;
import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;
import java.util.Arrays;

final class DistanceInterpolator {
   private final long[] deepVisionTimes;
   private final float[] athwartDistanceMeters;

   DistanceInterpolator(long[] deepVisionTimes, float[] athwartDistanceMeters) {
      this.deepVisionTimes = deepVisionTimes;
      this.athwartDistanceMeters = athwartDistanceMeters;
   }

   static float computeAthwartDistance(DataFileSet lsssDataFileSet, @Nullable GeoPoint geoPos, PingIndex pingIndex) {
      if (geoPos == null) {
         return 0;
      }
      GeoPoint pingIndexGeoPos = pingIndex.getGeographicalPosition();
      if (pingIndexGeoPos == null) {
         return 0;
      }
      GeoPoint metersPerGeoDegree = Earth.getMetersPerGeoDegree(geoPos);
      Point2D tangentAsPoint = DataUtils.getTangent(lsssDataFileSet, metersPerGeoDegree, pingIndex);
      if (tangentAsPoint == null) {
         return 0;
      }
      Vec2 tangent = new Vec2(tangentAsPoint);
      Vec2 posToNearest = new Vec2((float) ((geoPos.getX() - pingIndexGeoPos.getX()) * metersPerGeoDegree.getX()), (float) ((geoPos.getY() - pingIndexGeoPos.getY()) * metersPerGeoDegree.getY()));
      Vec2 posToTangent = posToNearest.minus(tangent.times(posToNearest.dot(tangent)));
      return posToTangent.cross(tangent);
   }

   private float interpolateDvToDistance(long time, int i) {
      long dvi = deepVisionTimes[i];
      long dvip1 = deepVisionTimes[i + 1];
      float disti = athwartDistanceMeters[i];
      float distip1 = athwartDistanceMeters[i + 1];
      long denominator = dvip1 - dvi;
      double w = (double) (time - dvi) / denominator;
      return (float) (disti + w * (distip1 - disti));
   }

   float deepVisionTimeToAthwartDistance(long deepVisionTime) {
      if (athwartDistanceMeters.length == 0) {
         return 0;
      }
      int i = Arrays.binarySearch(deepVisionTimes, deepVisionTime);
      if (i < 0) {
         // not exact match
         i = -(i + 1); // conversion to insertion point
         if (i == 0) {
            return athwartDistanceMeters[0];
         }
         i = i - 1;
         if (i >= deepVisionTimes.length - 1) {
            return 0;
         }
         return interpolateDvToDistance(deepVisionTime, i);
      }
      return athwartDistanceMeters[i];
   }
}
