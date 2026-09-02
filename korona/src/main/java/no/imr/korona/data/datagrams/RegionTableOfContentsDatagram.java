package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.TableOfContentsPingItem;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

/**
 * References to {@link RegionInfoDatagram}s.
 */
public final class RegionTableOfContentsDatagram extends DatagramPingItem implements TableOfContentsPingItem {
   public static final DatagramType TYPE = DatagramType.simple("RTC0", RegionTableOfContentsDatagram::new);

   private final List<Instant> instants;

   /**
    * Creates a new RegionTableOfContentsDatagram.
    *
    * @param instant           time for datagram
    * @param regionInfoInstants times of pings with RegionInfoDatagram
    */
   public RegionTableOfContentsDatagram(Instant instant, List<Instant> regionInfoInstants) {
      super(instant);

      instants = regionInfoInstants;
   }

   /**
    * Read one RTC0 datagram.
    *
    * @param instant    time for datagram
    * @param byteBuffer buffer to get from
    * @throws DatagramFormatException when parsing fails
    */
   public RegionTableOfContentsDatagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      instants = ByteBufferUtils.readInstantsAsNTDates(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeInstantsAsNTDates(byteBuffer, instants);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public List<Instant> getInstants() {
      return instants;
   }
}
