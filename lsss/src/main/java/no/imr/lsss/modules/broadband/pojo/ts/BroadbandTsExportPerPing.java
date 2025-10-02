package no.imr.lsss.modules.broadband.pojo.ts;

import no.imr.korona.data.ping.PingIndex;

import java.util.ArrayList;
import java.util.List;

public final class BroadbandTsExportPerPing {
   public long number;
   public String time;
   public List<BroadbandTsExportPerChannel> channels = new ArrayList<>();

   public BroadbandTsExportPerPing(PingIndex pingIndex) {
      number = pingIndex.getPingNumber();
      time = pingIndex.getInstant().toString();
   }
}
