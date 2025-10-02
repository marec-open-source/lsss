package no.imr.lsss.server.pojo.events;

import no.imr.korona.data.ping.PingIndex;
import org.jspecify.annotations.Nullable;

public final class EventEchogramPos {
   public String time;
   public @Nullable Float depth;

   public EventEchogramPos(PingIndex pingIndex, @Nullable Float depth) {
      time = pingIndex.getInstant().toString();
      this.depth = depth;
   }

   @Override
   public String toString() {
      return "EchogramPos{" +
            "time=" + time +
            ", depth=" + depth +
            '}';
   }
}
