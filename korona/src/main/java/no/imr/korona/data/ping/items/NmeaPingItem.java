package no.imr.korona.data.ping.items;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Nme0Datagram;
import no.imr.korona.data.util.Nmea;
import no.marec.lsss.api.util.GeoPoint;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class NmeaPingItem extends AbstractPingItem {
   private final String nmea;
   private final boolean write;

   public NmeaPingItem(long ntDate, String nmea, boolean write) {
      super(ntDate);

      this.nmea = nmea;
      this.write = write;
   }

   public NmeaPingItem(long ntDate, String nmea) {
      this(ntDate, nmea, true);
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      return write ? List.of(new Nme0Datagram(getNTDate(), nmea)) : List.of();
   }

   @Override
   public PingItem makeCopy() {
      return new NmeaPingItem(getNTDate(), nmea, write);
   }

   public String getNmeaString() {
      return nmea;
   }

   public Nmea getNmea() {
      return Nmea.of(nmea);
   }

   public OptionalDouble getKnots() {
      return getNmea().getKnots();
   }

   public Optional<GeoPoint> getGeographicalPosition() {
      return getNmea().getGeographicalPosition();
   }

   public OptionalDouble getHeading() {
      return getNmea().getHeading();
   }

   public OptionalDouble getVesselDistance() {
      return getNmea().getVesselDistance();
   }
}
