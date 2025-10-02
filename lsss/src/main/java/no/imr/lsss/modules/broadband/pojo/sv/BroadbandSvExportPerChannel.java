package no.imr.lsss.modules.broadband.pojo.sv;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class BroadbandSvExportPerChannel {
   public String id;
   public float nominalFrequency;
   public @Nullable Float minFrequency;
   public @Nullable Float maxFrequency;
   public @Nullable Integer numFrequencies;
   public float @Nullable [] sv;
   public @Nullable Float depth;
   public @Nullable String error;

   public BroadbandSvExportPerChannel(String id) {
      this.id = id;
   }
}
