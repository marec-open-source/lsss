package no.imr.lsss.modules.broadband.pojo.sv;

import no.imr.korona.data.ping.PingIndex;

import java.util.ArrayList;
import java.util.List;

public final class BroadbandSvExportPerPing {
   public long number;
   public String time;
   public List<BroadbandSvExportPerChannel> channels = new ArrayList<>();

   public BroadbandSvExportPerPing(PingIndex pingIndex) {
      number = pingIndex.getPingNumber();
      time = pingIndex.getInstant().toString();
   }
}
