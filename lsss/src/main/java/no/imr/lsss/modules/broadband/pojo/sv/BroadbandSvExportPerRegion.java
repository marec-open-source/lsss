package no.imr.lsss.modules.broadband.pojo.sv;

import no.imr.lsss.framework.export.pojo.ExportScrutiny;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BroadbandSvExportPerRegion {
   public int objectNumber;
   public Set<String> labels;
   public ExportScrutiny scrutiny;
   public List<BroadbandSvExportPerPing> pings = new ArrayList<>();
   public List<BroadbandSvExportPerChannel> averages = new ArrayList<>();

   public BroadbandSvExportPerRegion(int objectNumber, Set<String> labels, ExportScrutiny scrutiny) {
      this.objectNumber = objectNumber;
      this.labels = labels;
      this.scrutiny = scrutiny;
   }
}
