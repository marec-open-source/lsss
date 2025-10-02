package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.data.ping.PingIndex;

public interface Predictor {
   StateVector predict(Track track, PingIndex pingIndex);
}
