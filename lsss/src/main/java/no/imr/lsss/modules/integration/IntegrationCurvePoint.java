package no.imr.lsss.modules.integration;

import no.imr.korona.data.ping.PingIndex;
import org.jspecify.annotations.Nullable;

/**
 * A point on the curve for accumulated s<sub>A</sub>.
 */
public record IntegrationCurvePoint(
      PingIndex pingIndex,
      @Nullable PingCache pingCache,
      float accumulatedDistance,
      float horizontallyIntegratedSvTotal,
      float horizontallyIntegratedSvPelagic,
      float horizontallyIntegratedSvBottom
) {
   public float getHorizontallyIntegratedSv(IntegrationArea integrationArea) {
      return switch (integrationArea) {
         case TOTAL -> horizontallyIntegratedSvTotal;
         case PELAGIC -> horizontallyIntegratedSvPelagic;
         case BOTTOM -> horizontallyIntegratedSvBottom;
      };
   }

   @Override
   public String toString() {
      return "[" + pingIndex.getPingNumber() + ", " + accumulatedDistance + ", " + horizontallyIntegratedSvTotal + "]";
   }
}
