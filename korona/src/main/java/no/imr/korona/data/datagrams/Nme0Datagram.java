package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.util.Nmea;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * NMEA datagram.
 */
public final class Nme0Datagram extends BaseDatagram {
   public static final DatagramType TYPE = DatagramType.simple("NME0", Nme0Datagram::new);

   private final String nmea;

   /**
    * Create a Nme0Datagram.
    *
    * @param instant time
    * @param nmea    the NMEA string
    */
   public Nme0Datagram(Instant instant, String nmea) {
      super(instant);

      this.nmea = nmea;
   }

   /**
    * Read in one nmea datagram.
    *
    * @param instant    time
    * @param byteBuffer buffer to get from
    */
   public Nme0Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant);

      nmea = ByteBufferUtils.readCString(byteBuffer, byteBuffer.remaining());
   }

   @Override
   public String toStringExtra() {
      Nmea parsed = Nmea.of(nmea);
      return "nmea: \"" + nmea + "\""
            + ", knots: " + parsed.getKnots().orElse(Double.NaN)
            + ", geoPos: " + parsed.getGeographicalPosition().map(p -> "[" + p.x + ", " + p.y + "]").orElse(null);
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
      pingConversion.addPingItem(new NmeaPingItem(getInstant(), nmea));
   }

   public String getNmea() {
      return nmea;
   }
}
