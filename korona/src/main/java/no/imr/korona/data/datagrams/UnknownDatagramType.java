package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;

/**
 * Datagram type for all unknown datagrams.
 */
public final class UnknownDatagramType extends DatagramType {
   public UnknownDatagramType(int intCode) {
      super(intCode);
   }

   @Override
   public UnknownDatagram createDatagram(long ntDate, ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) {
      return new UnknownDatagram(ntDate, this, byteBuffer);
   }
}
