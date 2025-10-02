package no.imr.korona.computation.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.imr.tools.ResourceUtils;

import java.util.List;

record Example(
      @JsonProperty("file") String file,
      @JsonProperty("description") String description
) {
   static final List<Example> EXAMPLES = List.of(ResourceUtils.getJson("no/imr/korona/resources/modules/plugin/examples.json", Example[].class));

   @Override
   public String toString() {
      return description;
   }

   String getImplementation() {
      return ResourceUtils.getString("no/imr/korona/resources/modules/plugin/" + file);
   }
}
