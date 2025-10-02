package no.imr.lsss.server.pojo.events;

import no.marec.lsss.api.util.GeoPoint;

public final class EventGeoPos {
   public double lon;
   public double lat;

   public EventGeoPos(GeoPoint geoPos) {
      lon = geoPos.getLongitude();
      lat = geoPos.getLatitude();
   }

   @Override
   public String toString() {
      return "GeoPos{" +
            "lon=" + lon +
            ", lat=" + lat +
            '}';
   }
}
