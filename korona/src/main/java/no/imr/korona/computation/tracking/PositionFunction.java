package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.math.linalg.Vec3;

public interface PositionFunction {
   void update(Ping ping);

   Measurement toMeasurement(StateVector stateVector);

   Vec3 toGlobalPosition(Measurement measurement);
}
