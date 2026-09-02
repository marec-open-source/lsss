package no.imr.korona.data.datagrams;

import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.SubDatagram;
import no.imr.korona.data.ping.items.PingConversion;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Container datagram for different types of {@link BaseSubDatagram}.
 */
public final class LsssDatagram extends BaseDatagram {
   public static final DatagramType TYPE = new DatagramType("LSSS", LsssDatagram::new);

   private final SubDatagram subDatagram;

   public LsssDatagram(Instant instant, ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) throws DatagramFormatException {
      super(instant);

      int subtype = 0xffff & byteBuffer.getShort();
      DatagramSubType datagramSubType = datagramTypeManager.getDatagramSubType(subtype);
      if (datagramSubType == null) {
         throw new DatagramFormatException(Integer.toString(subtype));
      }
      subDatagram = datagramSubType.createSubDatagram(instant, byteBuffer);
   }

   public LsssDatagram(SubDatagram subDatagram) {
      super(subDatagram.getInstant());

      this.subDatagram = subDatagram;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putShort((short) subDatagram.getDatagramSubType().intCode());
      subDatagram.write(byteBuffer);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      subDatagram.addPingItems(pingConversion);
   }

   @Override
   public boolean isSampleDatagram() {
      return subDatagram.isSampleDatagram();
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   public SubDatagram getSubDatagram() {
      return subDatagram;
   }

   @Override
   public LsssDatagram makeCopy() {
      return new LsssDatagram(subDatagram.makeCopy());
   }

   @Override
   public String toStringExtra() {
      return subDatagram.getDatagramSubType().intCode() + " " + subDatagram.toStringExtra();
   }
}
