package no.imr.lsss.modules.broadband.pojo;

import java.util.List;
import java.util.Map;

public final class ExportChannelInfo {
   public String id;
   public List<Map<String, Object>> parameters;

   public ExportChannelInfo(String id, List<Map<String, Object>> parameters) {
      this.id = id;
      this.parameters = parameters;
   }
}
