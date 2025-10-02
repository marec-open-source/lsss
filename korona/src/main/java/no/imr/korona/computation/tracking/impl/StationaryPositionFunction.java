package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.PositionFunction;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.math.linalg.Vec3;

public final class StationaryPositionFunction implements PositionFunction {
   public StationaryPositionFunction() {
   }

   @Override
   public void update(Ping ping) {
   }

   @Override
   public Measurement toMeasurement(StateVector stateVector) {
      return globalPositionToMeasurement(stateVector.position(), stateVector.ts());
   }

   static Measurement globalPositionToMeasurement(Vec3 position, float ts) {
      float range = position.length();
      float along = (float) Math.atan2(position.x(), position.z());
      float athwart = (float) Math.atan2(position.y(), position.z());
      return new Measurement(range, along, athwart, ts);
   }

   @Override
   public Vec3 toGlobalPosition(Measurement measurement) {
      return measurementToGlobalPosition(measurement);
   }

   public static Vec3 measurementToGlobalPosition(Measurement measurement) {
      double a = Math.tan(measurement.alongshipAngle());
      double b = Math.tan(measurement.athwartshipAngle());
      double z = measurement.range() / Math.sqrt(1 + a * a + b * b);
      double x = z * a;
      double y = z * b;

      return new Vec3((float) x, (float) y, (float) z);
   }
}
