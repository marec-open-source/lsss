package no.imr.korona.data.ping.items;

public abstract class AbstractPingItem implements PingItem {
   private long ntDate;

   protected AbstractPingItem(long ntDate) {
      this.ntDate = ntDate;
   }

   @Override
   public long getNTDate() {
      return ntDate;
   }

   @Override
   public void setNTDate(long ntDate) {
      this.ntDate = ntDate;
   }
}
