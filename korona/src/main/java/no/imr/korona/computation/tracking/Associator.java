package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.computation.tracking.data.Track;

import java.util.Collection;
import java.util.List;

public interface Associator {
   void associate(Collection<Track> tracks, List<TargetCandidate> targetCandidates, GateFunction gateFunction);

   int getMaxMissingSamples();
}
