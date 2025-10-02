package no.imr.korona.data.buffer;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * A cyclic list of pings.
 */
public class CyclicPingList {
   private final PingBuffer pingBuffer;
   private final List<@Nullable Ping> pings;
   private int next;

   public CyclicPingList(PingBuffer pingBuffer, int size) {
      this.pingBuffer = pingBuffer;
      pings = Arrays.asList(new Ping[size]);
   }

   public List<@Nullable Ping> getPings() {
      return pings;
   }

   public int getNext() {
      return next;
   }

   public int index(int i) {
      return Utils.mod(i, getSize());
   }

   public int getSize() {
      return pings.size();
   }

   private @Nullable Ping getEndPing() {
      return pings.get(index(next - 1));
   }

   public void setEndPing(PingIndex endPingIndex) {
      Ping endPing = getEndPing();

      int n = getSize();

      int d;
      if (endPing == null) {
         d = -n;
      } else {
         d = (int) (endPingIndex.getPingNumber() - endPing.getPingIndex().getPingNumber());
      }

      d = Math.clamp(d, -n, n);

      if (d >= 0) {
         int a = next;
         for (int i = -d + 1; i <= 0; i++) {
            Ping ping = pingBuffer.getPing(endPingIndex, i);
            pings.set(next, ping);
            next = index(next + 1);
         }
         updated(a, d);
      } else { // d < 0
         for (int i = d + 1; i <= 0; i++) {
            Ping ping = pingBuffer.getPing(endPingIndex, -n - i + 1);
            next = index(next - 1);
            pings.set(next, ping);
         }
         updated(next, -d);
      }
   }

   protected void updated(int begin, int count) {
   }
}
