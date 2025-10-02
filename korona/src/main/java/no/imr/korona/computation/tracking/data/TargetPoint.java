package no.imr.korona.computation.tracking.data;

import no.imr.korona.computation.tracking.PositionFunction;

public record TargetPoint(StateVector stateVector, Measurement measurement) {

   public TargetPoint(StateVector stateVector, PositionFunction positionFunction) {
      this(stateVector, positionFunction.toMeasurement(stateVector));
   }
}
