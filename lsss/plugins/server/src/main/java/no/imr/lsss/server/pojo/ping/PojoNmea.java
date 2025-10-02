package no.imr.lsss.server.pojo.ping;

import no.imr.korona.data.ping.items.NmeaPingItem;

public final class PojoNmea {
   public String time;
   public String nmea;

   public PojoNmea(NmeaPingItem nmeaPingItem) {
      time = nmeaPingItem.getInstant().toString();
      nmea = nmeaPingItem.getNmeaString();
   }

   @Override
   public String toString() {
      return "PojoNmea{" +
            "time='" + time + '\'' +
            ", nmea='" + nmea + '\'' +
            '}';
   }
}
