package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.TableOfContentsPingItem;

import java.nio.ByteBuffer;

/**
 * References to {@link RegionInfoDatagram}s.
 */
public final class RegionTableOfContentsDatagram extends DatagramPingItem implements TableOfContentsPingItem {
   public static final DatagramType TYPE = DatagramType.simple("RTC0", RegionTableOfContentsDatagram::new);

   private final long[] ntDates;

   /**
    * Creates a new RegionTableOfContentsDatagram.
    *
    * @param ntDate            time for datagram
    * @param regionInfoNTDates times of pings with RegionInfoDatagram
    */
   public RegionTableOfContentsDatagram(long ntDate, long[] regionInfoNTDates) {
      super(ntDate);

      ntDates = regionInfoNTDates;
   }

   /**
    * Read one RTC0 datagram.
    *
    * @param ntDate     time for datagram
    * @param byteBuffer buffer to get from
    * @throws DatagramFormatException when parsing fails
    */
   public RegionTableOfContentsDatagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      ntDates = ByteBufferUtils.readCountAndLongArray(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCountAndLongArray(byteBuffer, ntDates);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public long[] getNTDates() {
      return ntDates;
   }
}
