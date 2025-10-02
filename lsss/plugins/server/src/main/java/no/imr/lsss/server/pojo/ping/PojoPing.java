package no.imr.lsss.server.pojo.ping;

import no.imr.korona.data.ping.Ping;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class PojoPing {
   public String time;
   public long pingNumber;
   public double vesselDistance;
   public @Nullable List<PojoNmea> nmea;
   public @Nullable List<PojoChannel> channels;

   public PojoPing(Ping ping) {
      time = ping.getInstant().toString();
      pingNumber = ping.getPingNumber();
      vesselDistance = ping.getVesselDistance();
   }

   @Override
   public String toString() {
      return "PojoPing{" +
            "time='" + time + '\'' +
            ", pingNumber=" + pingNumber +
            ", vesselDistance=" + vesselDistance +
            ", nmea=" + nmea +
            ", channels=" + channels +
            '}';
   }
}
