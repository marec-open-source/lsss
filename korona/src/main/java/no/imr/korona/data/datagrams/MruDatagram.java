package no.imr.korona.data.datagrams;

public abstract class MruDatagram extends DatagramPingItem {
   MruDatagram(long ntDate) {
      super(ntDate);
   }

   public abstract float getHeave();

   public abstract float getRoll();

   public abstract float getPitch();

   public abstract float getHeading();
}
