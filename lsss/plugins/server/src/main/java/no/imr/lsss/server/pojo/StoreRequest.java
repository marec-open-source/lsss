package no.imr.lsss.server.pojo;

import no.imr.tools.annotations.ReflectionEntryPoint;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class StoreRequest {
   public @Nullable Short quality;
   public @Nullable List<Integer> frequencies;

   @ReflectionEntryPoint
   public StoreRequest() {
   }

   public StoreRequest(Short quality, List<Integer> frequencies) {
      this.quality = quality;
      this.frequencies = frequencies;
   }

   @Override
   public String toString() {
      return "StoreRequest{" +
            "quality=" + quality +
            ", frequencies=" + frequencies +
            '}';
   }
}
