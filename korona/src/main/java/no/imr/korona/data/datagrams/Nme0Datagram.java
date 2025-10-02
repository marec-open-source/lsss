package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.ping.items.PingConversion;

import java.nio.ByteBuffer;

/**
 * NMEA datagram.
 */
public final class Nme0Datagram extends BaseDatagram {
   public static final DatagramType TYPE = new DatagramType.Simple("NME0", Nme0Datagram::new);

   private final String nmea;

   /**
    * Create a Nme0Datagram.
    *
    * @param ntDate time
    * @param nmea   the NMEA string
    */
   public Nme0Datagram(long ntDate, String nmea) {
      super(ntDate);

      this.nmea = nmea;
   }

   /**
    * Read in one nmea datagram.
    *
    * @param ntDate     time
    * @param byteBuffer buffer to get from
    */
   public Nme0Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate);

      nmea = ByteBufferUtils.readCString(byteBuffer, byteBuffer.remaining());
   }

   @Override
   public String toStringExtra() {
      return nmea;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCString(byteBuffer, nmea);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      pingConversion.addPingItem(new NmeaPingItem(getNTDate(), nmea));
   }

   public String getNmea() {
      return nmea;
   }
}
