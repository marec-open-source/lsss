package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * System information.
 */
public final class Sin0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = new DatagramType.Simple("SIN0", Sin0Datagram::new);

   public final List<Transceiver> transceivers;

   public Sin0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      transceivers = ByteBufferUtils.readCountAndList(byteBuffer, 4 + 2 + 32, Transceiver::new);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCountAndList(byteBuffer, transceivers, Transceiver::write);
   }

   @Override
   public String toStringExtra() {
      return "transceivers: " + transceivers;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   public static final class Transceiver {
      public final int ip;
      public final short port;
      public final String name;

      public Transceiver(ByteBuffer byteBuffer) {
         ip = byteBuffer.getInt();
         port = byteBuffer.getShort();
         name = ByteBufferUtils.readCString(byteBuffer, 32);
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(ip);
         byteBuffer.putShort(port);
         ByteBufferUtils.writeCString(byteBuffer, name, 32);
      }

      @Override
      public String toString() {
         return "{ip: " + ip + ", port: " + port + ", name: " + name + '}';
      }
   }
}
