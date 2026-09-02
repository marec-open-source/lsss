package no.imr.deepvision.lsss.engine.mapping;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TimeInterpolatorTest {
   @Test
   void test() {
      Instant[] deepVisionTimes = {
            Instant.ofEpochMilli(1000),
            Instant.ofEpochMilli(2000),
            Instant.ofEpochMilli(3000),
      };
      Instant[] lsssTimes = {
            Instant.ofEpochMilli(2000),
            Instant.ofEpochMilli(3000),
            Instant.ofEpochMilli(4000),
      };
      TimeInterpolator timeInterpolator = new TimeInterpolator(deepVisionTimes, lsssTimes);
      assertEquals(Instant.ofEpochMilli(0), timeInterpolator.deepVisionTimeToLsssTime(Instant.ofEpochMilli(0)));
      assertEquals(Instant.ofEpochMilli(2000), timeInterpolator.deepVisionTimeToLsssTime(Instant.ofEpochMilli(1000)));
      assertEquals(Instant.ofEpochMilli(2100), timeInterpolator.deepVisionTimeToLsssTime(Instant.ofEpochMilli(1100)));
      assertEquals(Instant.ofEpochMilli(2900), timeInterpolator.deepVisionTimeToLsssTime(Instant.ofEpochMilli(1900)));
      assertEquals(Instant.ofEpochMilli(4000), timeInterpolator.deepVisionTimeToLsssTime(Instant.ofEpochMilli(3000)));
      assertEquals(Instant.ofEpochMilli(4000), timeInterpolator.deepVisionTimeToLsssTime(Instant.ofEpochMilli(4000)));

      assertEquals(Instant.ofEpochMilli(0), timeInterpolator.lsssTimeToDeepVisionTime(Instant.ofEpochMilli(0)));
      assertEquals(Instant.ofEpochMilli(1000), timeInterpolator.lsssTimeToDeepVisionTime(Instant.ofEpochMilli(2000)));
      assertEquals(Instant.ofEpochMilli(1100), timeInterpolator.lsssTimeToDeepVisionTime(Instant.ofEpochMilli(2100)));
      assertEquals(Instant.ofEpochMilli(1800), timeInterpolator.lsssTimeToDeepVisionTime(Instant.ofEpochMilli(2800)));
      assertEquals(Instant.ofEpochMilli(3000), timeInterpolator.lsssTimeToDeepVisionTime(Instant.ofEpochMilli(4000)));
      assertEquals(Instant.ofEpochMilli(5000), timeInterpolator.lsssTimeToDeepVisionTime(Instant.ofEpochMilli(5000)));
   }
}
