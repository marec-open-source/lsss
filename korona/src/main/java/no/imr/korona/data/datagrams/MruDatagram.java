package no.imr.korona.data.datagrams;

import java.time.Instant;

public abstract class MruDatagram extends DatagramPingItem {
   MruDatagram(Instant instant) {
      super(instant);
   }

   public abstract float getHeave();

   public abstract float getRoll();

   public abstract float getPitch();

   public abstract float getHeading();
}
