package no.imr.lsss.modules.trawl.spd;

import java.util.List;

public record SpdStation(
      SLine sLine,
      List<SpdTarget> targets
) {
   @Override
   public String toString() {
      return sLine.parseTime(sLine.startTime) + ", " + targets.size() + " targets";
   }
}
