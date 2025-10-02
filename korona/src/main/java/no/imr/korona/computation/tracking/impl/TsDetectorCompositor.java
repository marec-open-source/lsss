package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Compositor;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;

import java.util.List;

public final class TsDetectorCompositor implements Compositor {
   public TsDetectorCompositor() {
   }

   @Override
   public Measurement compose(List<TargetCandidate> targetCandidates) {
      assert targetCandidates.size() == 1;
      return targetCandidates.getFirst().getMeasurement();
   }
}
