package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.range.FloatRange;

import java.util.List;

public interface TargetCandidateExtractor {
   List<TargetCandidate> extract(Ping ping, FloatRange depthRange);
}
