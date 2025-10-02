package no.imr.lsss.server.pojo;

import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.export.ExportUtils;
import no.imr.lsss.framework.export.pojo.ExportScrutiny;
import no.imr.lsss.server.util.LsssServerUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class RegionInfo {
   public int id;
   public @Nullable String type;
   public @Nullable Set<String> labels;
   public @Nullable ExportScrutiny scrutiny;
   public @Nullable List<ApiEchogramPoint> boundingBox;

   public RegionInfo(int id) {
      this.id = id;
   }

   public RegionInfo(Region region, LSSS lsss) {
      id = region.getObjectNumber();
      type = toType(region);
      labels = region.getLabels();
      scrutiny = ExportUtils.makeScrutiny(lsss, region.getInterpretation());
      boundingBox = LsssServerUtils.toBoundingBox(region, lsss);
   }

   public static String toType(Region region) {
      return region instanceof School ? "school" : "layer";
   }

   @Override
   public String toString() {
      return "RegionInfo{" +
            "id=" + id +
            ", type='" + type + '\'' +
            ", labels=" + labels +
            '}';
   }
}
