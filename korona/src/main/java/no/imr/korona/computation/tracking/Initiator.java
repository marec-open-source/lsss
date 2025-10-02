package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.TargetCandidate;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.data.ping.Ping;

import java.util.List;

public interface Initiator {
   List<Track> initiateNewTracks(Ping ping, List<TargetCandidate> targetCandidates, PositionFunction positionFunction, TrackIdGenerator trackIdGenerator);
}
