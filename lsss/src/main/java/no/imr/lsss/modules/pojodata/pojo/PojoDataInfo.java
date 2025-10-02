package no.imr.lsss.modules.pojodata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

public final class PojoDataInfo {
   public String id;
   public String time = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public Map<String, String> units = new TreeMap<>();

   public PojoDataInfo(String id) {
      this.id = id;
   }
}
