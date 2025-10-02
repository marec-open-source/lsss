package no.imr.lsss.modules.trawl.spd;

import java.util.List;

public record SpdTarget(
      TLine tLine,
      List<ULine> uLines,
      List<VLine> vLines
) {
   @Override
   public String toString() {
      return tLine.speciesName + ", " + uLines.size() + " uLines, " + vLines.size() + " vLines";
   }
}
