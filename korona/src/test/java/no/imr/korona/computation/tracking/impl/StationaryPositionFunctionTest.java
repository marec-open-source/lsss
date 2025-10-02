package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

final class StationaryPositionFunctionTest {
   @Test
   void test() {
      check(new Vec3(0, 0, 1), new Measurement(1, 0, 0, 0));
      check(new Vec3(1, 0, 1), new Measurement((float) Math.sqrt(2), (float) Math.toRadians(45), 0, 0));
      check(new Vec3(0, 1, 1), new Measurement((float) Math.sqrt(2), 0, (float) Math.toRadians(45), 0));
      check(new Vec3(1, 1, 1), new Measurement((float) Math.sqrt(3), (float) Math.toRadians(45), (float) Math.toRadians(45), 0));

      check(new Vec3(-1, -1, 1));
      check(new Vec3(-1, 0, 1));
      check(new Vec3(-1, 1, 1));

      check(new Vec3(0, -1, 1));
      check(new Vec3(0, 0, 1));
      check(new Vec3(0, 1, 1));

      check(new Vec3(1, -1, 1));
      check(new Vec3(1, 0, 1));
      check(new Vec3(1, 1, 1));
   }

   private static void check(Vec3 expectedPos, Measurement measurement) {
      Vec3 actualPos = StationaryPositionFunction.measurementToGlobalPosition(measurement);
      JUnitUtils.assertEquals(expectedPos, actualPos, 1e-6f);
   }

   private static void check(Vec3 pos) {
      check(pos, StationaryPositionFunction.globalPositionToMeasurement(pos, 0));
   }
}
