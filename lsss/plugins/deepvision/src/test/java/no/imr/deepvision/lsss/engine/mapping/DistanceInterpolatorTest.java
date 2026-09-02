package no.imr.deepvision.lsss.engine.mapping;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class DistanceInterpolatorTest {
   @Test
   void test() {
      Instant[] deepVisionTimes = {
            Instant.ofEpochMilli(1000),
            Instant.ofEpochMilli(2000),
            Instant.ofEpochMilli(3000)
      };
      float[] distances = {2, 3, 4};

      DistanceInterpolator distanceInterpolator = new DistanceInterpolator(deepVisionTimes, distances);
      assertEquals(2, distanceInterpolator.deepVisionTimeToAthwartDistance(Instant.ofEpochMilli(0)));
      assertEquals(2, distanceInterpolator.deepVisionTimeToAthwartDistance(Instant.ofEpochMilli(1000)));
      assertEquals(2.1f, distanceInterpolator.deepVisionTimeToAthwartDistance(Instant.ofEpochMilli(1100)));
      assertEquals(2.9f, distanceInterpolator.deepVisionTimeToAthwartDistance(Instant.ofEpochMilli(1900)));
      assertEquals(3.9f, distanceInterpolator.deepVisionTimeToAthwartDistance(Instant.ofEpochMilli(2900)));
      assertEquals(4, distanceInterpolator.deepVisionTimeToAthwartDistance(Instant.ofEpochMilli(3000)));
      assertEquals(4, distanceInterpolator.deepVisionTimeToAthwartDistance(Instant.ofEpochMilli(4000)));
   }
}
