package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Ping information.
 */
public sealed class Pin0Datagram extends DatagramPingItem permits Pin1Datagram {
   public static final DatagramType TYPE_PIN0 = DatagramType.simple("PIN0", Pin0Datagram::new);

   public final long pingNTDate;
   public final int pingNumber;
   public final double latitude;
   public final double longitude;
   public final double speed;
   public final double heading;
   public final double heave;
   public final double roll;
   public final double pitch;
   public final double vesselDepth;
   public final boolean vesselDistancePresent;
   public final double vesselDistance;
   public final double transducerOffsetX;
   public final double transducerOffsetY;
   public final double transducerOffsetZ;
   public final double relativeTransducerHeading;
   public final double soundVelocity;

   public Pin0Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant);

      pingNTDate = byteBuffer.getLong();
      pingNumber = byteBuffer.getInt();
      latitude = byteBuffer.getDouble();
      longitude = byteBuffer.getDouble();
      speed = byteBuffer.getDouble();
      heading = byteBuffer.getDouble();
      heave = byteBuffer.getDouble();
      roll = byteBuffer.getDouble();
      pitch = byteBuffer.getDouble();
      vesselDepth = byteBuffer.getDouble();
      vesselDistancePresent = byteBuffer.remaining() >= 6 * 8;
      vesselDistance = vesselDistancePresent ? byteBuffer.getDouble() : Double.NaN;
      transducerOffsetX = byteBuffer.getDouble();
      transducerOffsetY = byteBuffer.getDouble();
      transducerOffsetZ = byteBuffer.getDouble();
      relativeTransducerHeading = byteBuffer.getDouble();
      soundVelocity = byteBuffer.getDouble();
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putLong(pingNTDate);
      byteBuffer.putInt(pingNumber);
      byteBuffer.putDouble(latitude);
      byteBuffer.putDouble(longitude);
      byteBuffer.putDouble(speed);
      byteBuffer.putDouble(heading);
      byteBuffer.putDouble(heave);
      byteBuffer.putDouble(roll);
      byteBuffer.putDouble(pitch);
      byteBuffer.putDouble(vesselDepth);
      if (vesselDistancePresent) {
         byteBuffer.putDouble(vesselDistance);
      }
      byteBuffer.putDouble(transducerOffsetX);
      byteBuffer.putDouble(transducerOffsetY);
      byteBuffer.putDouble(transducerOffsetZ);
      byteBuffer.putDouble(relativeTransducerHeading);
      byteBuffer.putDouble(soundVelocity);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE_PIN0;
   }

   @Override
   public String toStringExtra() {
      return "pingNTDate: " + pingNTDate +
            ", pingNumber: " + pingNumber +
            ", latitude: " + latitude +
            ", longitude: " + longitude +
            ", speed: " + speed +
            ", heading: " + heading +
            ", heave: " + heave +
            ", roll: " + roll +
            ", pitch: " + pitch +
            ", vesselDepth: " + vesselDepth +
            ", transducerOffsetX: " + transducerOffsetX +
            ", transducerOffsetY: " + transducerOffsetY +
            ", transducerOffsetZ: " + transducerOffsetZ +
            ", relativeTransducerHeading: " + relativeTransducerHeading +
            ", soundVelocity: " + soundVelocity;
   }
}
