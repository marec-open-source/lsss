package no.imr.lsss.framework.export.pojo;

import no.imr.lsss.LSSS;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Meta information about the export.
 * <p>
 * Units should use names defined by
 * <a href="http://www.unidata.ucar.edu/software/udunits/">http://www.unidata.ucar.edu/software/udunits/</a>.
 */
public final class ExportInfo {
   public String exportType;
   public String exportTime = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
   public String lsssVersion = LSSS.VERSION;
   public String formatLastChangedInLsssVersion;
   public Map<String, Object> parameters = new LinkedHashMap<>();
   public Map<String, String> units = new TreeMap<>();
   public List<String> comments = new ArrayList<>();

   public ExportInfo(String formatLastChangedInLsssVersion, String exportType) {
      this.formatLastChangedInLsssVersion = formatLastChangedInLsssVersion;
      this.exportType = exportType;
      comments.add("The format of and definitions in this file may change in future versions of LSSS");
   }
}
