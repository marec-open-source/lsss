package no.imr.korona.data.datagrams;

import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.SubDatagram;
import no.imr.korona.data.ping.items.PingConversion;

import java.nio.ByteBuffer;

/**
 * Container datagram for different types of {@link BaseSubDatagram}.
 */
public final class LsssDatagram extends BaseDatagram {
   public static final DatagramType TYPE = new DatagramType("LSSS") {
      @Override
      public BaseDatagram createDatagram(long ntDate, ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) throws DatagramFormatException {
         return new LsssDatagram(ntDate, byteBuffer, datagramTypeManager);
      }
   };

   private final SubDatagram subDatagram;

   public LsssDatagram(long ntDate, ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) throws DatagramFormatException {
      super(ntDate);

      int subtype = 0xffff & byteBuffer.getShort();
      DatagramSubType datagramSubType = datagramTypeManager.getDatagramSubType(subtype);
      if (datagramSubType == null) {
         throw new DatagramFormatException(Integer.toString(subtype));
      }
      subDatagram = datagramSubType.createSubDatagram(ntDate, byteBuffer);
   }

   public LsssDatagram(SubDatagram subDatagram) {
      super(subDatagram.getNTDate());

      this.subDatagram = subDatagram;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putShort((short) subDatagram.getDatagramSubType().getIntCode());
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
      return subDatagram.getDatagramSubType().getIntCode() + " " + subDatagram.toStringExtra();
   }
}
