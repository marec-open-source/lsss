package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;

public final class Mru1Datagram extends MruDatagram {
   public static final DatagramType TYPE = new DatagramType.Simple("MRU1", Mru1Datagram::new);

   public final String startId;                 // Start ID                #KMB     char     4U
   public final short datagramLength;           // Datagram length                  uint16   2U
   public final short datagramVersion;          // Datagram version (=1)            uint16   2U
   public final int utcSeconds;                 // UTC seconds             s        uint32   4U
   public final int utcNanoseconds;             // UTC nanoseconds         ns       uint32   4U
   public final int status;                     // Status                           uint32   4U
   public final double latitude;                // Latitude                deg      double   8F
   public final double longitude;               // Longitude               deg      double   8F
   public final float ellipsoidHeight;          // Ellipsoid height        m        float    4F
   public final float roll;                     // Roll                    deg      float    4F
   public final float pitch;                    // Pitch                   deg      float    4F
   public final float heading;                  // Heading                 deg      float    4F
   public final float heave;                    // Heave                   m        float    4F
   public final float rollRate;                 // Roll rate               deg/s    float    4F
   public final float pitchRate;                // Pitch rate              deg/s    float    4F
   public final float yawRate;                  // Yaw rate                deg/s    float    4F
   public final float northVelocity;            // North velocity          m/s      float    4F
   public final float eastVelocity;             // East velocity           m/s      float    4F
   public final float downVelocity;             // Down velocity           m/s      float    4F
   public final float latitudeError;            // Latitude error          m        float    4F
   public final float longitudeError;           // Longitude error         m        float    4F
   public final float heightError;              // Height error            m        float    4F
   public final float rollError;                // Roll error              deg      float    4F
   public final float pitchError;               // Pitch error             deg      float    4F
   public final float headingError;             // Heading error           deg      float    4F
   public final float heaveError;               // Heave error             m        float    4F
   public final float northAcceleration;        // North acceleration      m/s2     float    4F
   public final float eastAcceleration;         // East acceleration       m/s2     float    4F
   public final float downAcceleration;         // Down acceleration       m/s2     float    4F
   //--------------------------------------------- Delayed heave:
   public final int delayedHeaveUtcSeconds;     // UTC seconds             s        uint32   4U
   public final int delayedHeaveUtcNanosecond;  // UTC nanosecond          ns       uint32   4U
   public final float delayedHeave;             // Delayed heave           m        float    4F

   public Mru1Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate);

      startId = ByteBufferUtils.readCString(byteBuffer, 4);
      datagramLength = byteBuffer.getShort();
      datagramVersion = byteBuffer.getShort();
      utcSeconds = byteBuffer.getInt();
      utcNanoseconds = byteBuffer.getInt();
      status = byteBuffer.getInt();
      latitude = byteBuffer.getDouble();
      longitude = byteBuffer.getDouble();
      ellipsoidHeight = byteBuffer.getFloat();
      roll = byteBuffer.getFloat();
      pitch = byteBuffer.getFloat();
      heading = byteBuffer.getFloat();
      heave = byteBuffer.getFloat();
      rollRate = byteBuffer.getFloat();
      pitchRate = byteBuffer.getFloat();
      yawRate = byteBuffer.getFloat();
      northVelocity = byteBuffer.getFloat();
      eastVelocity = byteBuffer.getFloat();
      downVelocity = byteBuffer.getFloat();
      latitudeError = byteBuffer.getFloat();
      longitudeError = byteBuffer.getFloat();
      heightError = byteBuffer.getFloat();
      rollError = byteBuffer.getFloat();
      pitchError = byteBuffer.getFloat();
      headingError = byteBuffer.getFloat();
      heaveError = byteBuffer.getFloat();
      northAcceleration = byteBuffer.getFloat();
      eastAcceleration = byteBuffer.getFloat();
      downAcceleration = byteBuffer.getFloat();
      delayedHeaveUtcSeconds = byteBuffer.getInt();
      delayedHeaveUtcNanosecond = byteBuffer.getInt();
      delayedHeave = byteBuffer.getFloat();
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCString(byteBuffer, startId, 4);
      byteBuffer.putShort(datagramLength);
      byteBuffer.putShort(datagramVersion);
      byteBuffer.putInt(utcSeconds);
      byteBuffer.putInt(utcNanoseconds);
      byteBuffer.putInt(status);
      byteBuffer.putDouble(latitude);
      byteBuffer.putDouble(longitude);
      byteBuffer.putFloat(ellipsoidHeight);
      byteBuffer.putFloat(roll);
      byteBuffer.putFloat(pitch);
      byteBuffer.putFloat(heading);
      byteBuffer.putFloat(heave);
      byteBuffer.putFloat(rollRate);
      byteBuffer.putFloat(pitchRate);
      byteBuffer.putFloat(yawRate);
      byteBuffer.putFloat(northVelocity);
      byteBuffer.putFloat(eastVelocity);
      byteBuffer.putFloat(downVelocity);
      byteBuffer.putFloat(latitudeError);
      byteBuffer.putFloat(longitudeError);
      byteBuffer.putFloat(heightError);
      byteBuffer.putFloat(rollError);
      byteBuffer.putFloat(pitchError);
      byteBuffer.putFloat(headingError);
      byteBuffer.putFloat(heaveError);
      byteBuffer.putFloat(northAcceleration);
      byteBuffer.putFloat(eastAcceleration);
      byteBuffer.putFloat(downAcceleration);
      byteBuffer.putInt(delayedHeaveUtcSeconds);
      byteBuffer.putInt(delayedHeaveUtcNanosecond);
      byteBuffer.putFloat(delayedHeave);
   }

   @Override
   public String toStringExtra() {
      return "startId: " + startId +
            ", heave: " + heave +
            ", roll: " + roll +
            ", pitch: " + pitch +
            ", heading: " + heading;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public float getHeave() {
      return heave;
   }

   @Override
   public float getRoll() {
      return roll;
   }

   @Override
   public float getPitch() {
      return pitch;
   }

   @Override
   public float getHeading() {
      return heading;
   }
}
