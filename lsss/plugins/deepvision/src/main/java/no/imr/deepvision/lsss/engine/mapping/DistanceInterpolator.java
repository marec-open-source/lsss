package no.imr.deepvision.lsss.engine.mapping;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.geo.Earth;
import no.imr.tools.math.MathUtils;
import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

final class DistanceInterpolator {
   private final Instant[] deepVisionTimes;
   private final float[] athwartDistanceMeters;

   DistanceInterpolator(Instant[] deepVisionTimes, float[] athwartDistanceMeters) {
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

   float deepVisionTimeToAthwartDistance(Instant deepVisionTime) {
      if (athwartDistanceMeters.length == 0) {
         return 0;
      }
      int i = Arrays.binarySearch(deepVisionTimes, deepVisionTime);
      if (i >= 0) {
         // Exact match.
         return athwartDistanceMeters[i];
      }
      i = -(i + 1); // Conversion to insertion point.
      if (i <= 0) {
         return athwartDistanceMeters[0];
      }
      if (i >= deepVisionTimes.length) {
         return athwartDistanceMeters[athwartDistanceMeters.length - 1];
      }
      Instant timeA = deepVisionTimes[i - 1];
      Instant timeB = deepVisionTimes[i];
      float distA = athwartDistanceMeters[i - 1];
      float distB = athwartDistanceMeters[i];
      float w = (float) timeA.until(deepVisionTime, ChronoUnit.NANOS) / timeA.until(timeB, ChronoUnit.NANOS);
      return (float) MathUtils.interpolate(distA, distB, w);
   }
}
