package no.imr.lsss.server.pojo;

import jakarta.ws.rs.QueryParam;
import no.imr.tools.annotations.ReflectionEntryPoint;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public final class RegionSelectionRequest {
   public Operation operation = Operation.SET;

   @QueryParam("all")
   public @Nullable Boolean all;

   @QueryParam("layers")
   public @Nullable Boolean layers;

   @QueryParam("schools")
   public @Nullable Boolean schools;

   @QueryParam("selected")
   public @Nullable Boolean selected;

   @QueryParam("visible")
   public @Nullable Boolean visible;

   public @Nullable Set<Integer> ids;

   @QueryParam("ids")
   public @Nullable String idsAsQueryParam;

   public @Nullable Set<String> labels;

   @QueryParam("labels")
   public @Nullable String labelsAsQueryParam;

   @ReflectionEntryPoint
   public RegionSelectionRequest() {
   }

   @Override
   public String toString() {
      return "RegionSelectionRequest{" +
            "operation=" + operation +
            ", all=" + all +
            ", schools=" + schools +
            ", layers=" + layers +
            ", selected=" + selected +
            ", visible=" + visible +
            ", ids=" + ids +
            ", idsAsQueryParam='" + idsAsQueryParam + '\'' +
            ", labels=" + labels +
            ", labelsAsQueryParam='" + labelsAsQueryParam + '\'' +
            '}';
   }

   public enum Operation {
      ADD, REMOVE, RETAIN, SET
   }
}
