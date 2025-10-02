package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

public final class TNF0Datagram extends DatagramPingItem implements TrackPingItem {
   public static final DatagramType TYPE = new DatagramType.Simple("TNF0", TNF0Datagram::new);

   private final int id;
   private int channel;
   private final boolean valid;
   private final int pingsSinceFirst;
   private final int pingsSinceLast;

   public TNF0Datagram(long ntDate, int id, int channel, boolean valid, int pingsSinceFirst, int pingsSinceLast) {
      super(ntDate);

      this.id = id;
      this.channel = channel;
      this.valid = valid;
      this.pingsSinceFirst = pingsSinceFirst;
      this.pingsSinceLast = pingsSinceLast;
   }

   public TNF0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      id = byteBuffer.getInt();
      channel = 0xffff & byteBuffer.getShort();
      valid = byteBuffer.get() != 0;
      pingsSinceFirst = byteBuffer.getInt();
      if (pingsSinceFirst < 0) {
         throw new DatagramFormatException(Integer.toString(pingsSinceFirst));
      }
      if (byteBuffer.hasRemaining()) {
         pingsSinceLast = byteBuffer.getInt();
         if (pingsSinceLast < 0) {
            throw new DatagramFormatException(Integer.toString(pingsSinceLast));
         }
      } else {
         pingsSinceLast = 0;
      }
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(id);
      byteBuffer.putShort((short) channel);
      byteBuffer.put((byte) (valid ? 1 : 0));
      byteBuffer.putInt(pingsSinceFirst);
      byteBuffer.putInt(pingsSinceLast);
   }

   @Override
   public String toStringExtra() {
      return "id: " + id +
            ", channel: " + channel +
            ", valid: " + valid +
            ", pingsSinceFirst: " + pingsSinceFirst +
            ", pingsSinceLast: " + pingsSinceLast;
   }


   @Override
   public int getId() {
      return id;
   }

   @Override
   public int getChannel() {
      return channel;
   }

   @Override
   public void setChannel(int channel) {
      this.channel = channel;
   }

   public boolean isValid() {
      return valid;
   }

   public int getPingsSinceFirst() {
      return pingsSinceFirst;
   }

   public int getPingsSinceLast() {
      return pingsSinceLast;
   }
}
