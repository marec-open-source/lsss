package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

/**
 * Class for unknown datagrams.
 * All datagrams not recognized by the system are represented by this datagram.
 */
public final class UnknownDatagram extends DatagramPingItem {
   private final DatagramType datagramType;
   private final byte[] contents;

   public UnknownDatagram(long ntDate, UnknownDatagramType datagramType, ByteBuffer byteBuffer) {
      super(ntDate);

      this.datagramType = datagramType;
      contents = new byte[byteBuffer.remaining()];
      byteBuffer.get(contents);
   }

   @Override
   public String toStringExtra() {
      return "Unknown datagram: " + contents.length + " bytes";
   }

   @Override
   public DatagramType getDatagramType() {
      return datagramType;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.put(contents);
   }
}
