package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.modules.integration.IntegrationArea;
import no.imr.lsss.modules.integration.IntegrationCurvePoint;
import no.imr.lsss.modules.integration.PingCache;
import no.imr.tools.math.Median;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes a s<sub>A</sub> curve, not accumulated.
 */
public record SaCurve(
      float maxSa,
      List<Point> points
) {
   public static SaCurve compute(List<IntegrationCurvePoint> curve, IntegrationArea integrationArea) {
      if (curve.isEmpty()) {
         return new SaCurve(0, List.of());
      }

      int n = curve.size();
      float[] originalSa = new float[n];
      for (int i = 0; i < n; i++) {
         IntegrationCurvePoint curvePoint = curve.get(i);
         PingCache pingCache = curvePoint.pingCache();
         originalSa[i] = pingCache != null ? pingCache.getVerticallyIntegratedSv(integrationArea) : 0;
      }

      int filterLen = 11;
      int halfFilterLen = (filterLen - 1) / 2;

      float[] medianFilteredSa = new float[n];
      float[] tmp = new float[filterLen];
      for (int i = 0; i < n; i++) {
         int iMin = Math.max(0, i - halfFilterLen);
         int iMax = Math.min(n - 1, i + halfFilterLen);
         int iCount = iMax - iMin + 1;
         System.arraycopy(originalSa, iMin, tmp, 0, iCount);
         medianFilteredSa[i] = Median.quickSelect(tmp, 0, iCount);
      }

      float[] meanMedianFilteredSa = new float[n];
      for (int i = 0; i < n; i++) {
         int iMin = Math.max(0, i - halfFilterLen);
         int iMax = Math.min(n - 1, i + halfFilterLen);
         int iCount = iMax - iMin + 1;
         float sum = 0;
         for (int j = iMin; j <= iMax; j++) {
            sum += medianFilteredSa[j];
         }
         meanMedianFilteredSa[i] = sum / iCount;
      }

      List<Point> points = new ArrayList<>(n);
      float maxSa = 0;
      for (int i = 0; i < n; i++) {
         float sa = meanMedianFilteredSa[i];
         points.add(new Point(curve.get(i).pingIndex(), sa));
         if (maxSa < sa) {
            maxSa = sa;
         }
      }
      return new SaCurve(maxSa, points);
   }

   @Override
   public String toString() {
      return points.size() + " points, maxSa = " + maxSa;
   }

   public record Point(PingIndex pingIndex, float sa) {
      @Override
      public String toString() {
         return pingIndex + ", " + sa;
      }
   }
}
