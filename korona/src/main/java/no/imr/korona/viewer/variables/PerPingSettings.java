package no.imr.korona.viewer.variables;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.range.FloatRange;

import java.util.Collections;
import java.util.NavigableSet;

public abstract class PerPingSettings {
   protected PerPingSettings() {
   }

   public abstract FloatRange getClipRange(PingIndex pingIndex);

   public NavigableSet<Float> getLowerThresholds() {
      return Collections.emptyNavigableSet();
   }

   public NavigableSet<Float> getUpperThresholds() {
      return Collections.emptyNavigableSet();
   }

   public boolean varyingClipAbove() {
      return false;
   }
}
