package no.imr.korona.region;

import com.google.common.collect.ImmutableMap;

/**
 * School parameters as an immutable value.
 */
public record SchoolParameters(
      boolean dataProcessed,
      ImmutableMap<String, Float> values,
      ImmutableMap<Integer, ImmutableMap<String, Float>> perChannelValues
) {
   static final SchoolParameters EMPTY = new SchoolParameters(false, ImmutableMap.of(), ImmutableMap.of());

   public boolean isUpToDate() {
      return !values.isEmpty();
   }
}
