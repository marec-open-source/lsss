package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

public final class Mru0Datagram extends MruDatagram {
   public static final DatagramType TYPE = new DatagramType.Simple("MRU0", Mru0Datagram::new);

   private final float heave;
   private final float roll;
   private final float pitch;
   private final float heading;

   public Mru0Datagram(long ntDate, float heave, float roll, float pitch, float heading) {
      super(ntDate);

      this.heave = heave;
      this.roll = roll;
      this.pitch = pitch;
      this.heading = heading;
   }

   public Mru0Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate);

      heave = byteBuffer.getFloat();
      roll = byteBuffer.getFloat();
      pitch = byteBuffer.getFloat();
      heading = byteBuffer.getFloat();
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putFloat(heave);
      byteBuffer.putFloat(roll);
      byteBuffer.putFloat(pitch);
      byteBuffer.putFloat(heading);
   }

   @Override
   public String toStringExtra() {
      return "heave: " + heave +
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
