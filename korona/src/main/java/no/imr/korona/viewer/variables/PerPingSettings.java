package no.imr.korona.viewer.variables;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.range.FloatRange;

import java.util.Collections;
import java.util.NavigableSet;

public interface PerPingSettings {
   FloatRange getClipRange(PingIndex pingIndex);

   default NavigableSet<Float> getLowerThresholds() {
      return Collections.emptyNavigableSet();
   }

   default NavigableSet<Float> getUpperThresholds() {
      return Collections.emptyNavigableSet();
   }

   default boolean varyingClipAbove() {
      return false;
   }
}
