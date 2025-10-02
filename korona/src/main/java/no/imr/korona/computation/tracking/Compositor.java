package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.TargetCandidate;

import java.util.List;

public interface Compositor {
   Measurement compose(List<TargetCandidate> targetCandidates);
}
