package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.PositionFunction;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.tools.math.linalg.Matrix3;
import no.imr.tools.math.linalg.TRS;
import no.imr.tools.math.linalg.Vec3;

public final class FloatingPositionFunction implements PositionFunction {
   private TRS trs = TRS.IDENTITY;
   private TRS trsInv = trs;

   public FloatingPositionFunction() {
   }

   @Override
   public void update(Ping ping) {
      float dz;
      Matrix3 rotation;

      ChannelData channelData = ping.getNonNullChannelData();
      if (channelData == null) {
         dz = 0;
         rotation = Matrix3.IDENTITY;
      } else {
         dz = channelData.getHeave();
         rotation = getRotation(channelData);
      }

      Vec3 translation = new Vec3(0, 0, dz);
      trs = new TRS(translation, rotation);
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

   private static Matrix3 getRotation(ChannelData channelData) {
      Matrix3 headingRotation = Matrix3.createRotation(90 - channelData.getHeading(), new Vec3(0, 0, 1));
      Matrix3 rollRotation = Matrix3.createRotation(channelData.getRoll(), new Vec3(1, 0, 0));
      Matrix3 pitchRotation = Matrix3.createRotation(-channelData.getPitch(), new Vec3(0, 1, 0));
      return headingRotation.multiply(pitchRotation).multiply(rollRotation);
   }
}
