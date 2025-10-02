package no.imr.lsss.modules.broadband.pojo.ts;

import java.util.ArrayList;
import java.util.List;

public final class BroadbandTsExportPerChannel {
   public String id;
   public float nominalFrequency;
   public float minFrequency;
   public float maxFrequency;
   public int numFrequencies;
   public List<BroadbandTsExportPerTarget> targets = new ArrayList<>();

   public BroadbandTsExportPerChannel(String id) {
      this.id = id;
   }
}
