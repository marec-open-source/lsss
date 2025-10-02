package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.PositionFunction;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.util.transform.AccumulatingTangentPingTransform;
import no.imr.korona.util.transform.PingTransform;
import no.imr.korona.util.transform.PingTransformWithTangent;
import no.imr.tools.math.linalg.TRS;
import no.imr.tools.math.linalg.Vec3;
import org.jspecify.annotations.Nullable;

public final class MovingPositionFunction implements PositionFunction {
   private final @Nullable DataFileSet dataFileSet;
   private @Nullable AccumulatingTangentPingTransform accumulatingTangentPingTransform;
   private @Nullable PingTransform pingTransform;
   private TRS trs = TRS.IDENTITY;
   private TRS trsInv = trs;

   public MovingPositionFunction() {
      this(null);
   }

   public MovingPositionFunction(@Nullable DataFileSet dataFileSet) {
      this.dataFileSet = dataFileSet;
   }

   @Override
   public void update(Ping ping) {
      if (pingTransform == null) {
         if (dataFileSet != null) {
            pingTransform = new PingTransformWithTangent(dataFileSet, ping);
         } else {
            accumulatingTangentPingTransform = new AccumulatingTangentPingTransform(ping);
            pingTransform = accumulatingTangentPingTransform;
         }
      }

      if (accumulatingTangentPingTransform != null) {
         accumulatingTangentPingTransform.add(ping.getPingIndex());
      }

      trs = pingTransform.getTransform(ping);
      trsInv = trs.inverse();
   }

   @Override
   public Measurement toMeasurement(StateVector stateVector) {
      Vec3 pos = trsInv.transformPoint(stateVector.position());
      return StationaryPositionFunction.globalPositionToMeasurement(pos, stateVector.ts());
   }

   @Override
   public Vec3 toGlobalPosition(Measurement measurement) {
      Vec3 pos = StationaryPositionFunction.measurementToGlobalPosition(measurement);
      return trs.transformPoint(pos);
   }
}
