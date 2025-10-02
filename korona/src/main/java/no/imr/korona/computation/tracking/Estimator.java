package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.computation.tracking.data.TrackPoint;

public interface Estimator {
   StateVector estimate(Track track, TrackPoint trackPoint, PositionFunction positionFunction);
}
