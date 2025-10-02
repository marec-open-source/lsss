package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.Measurement;

public interface GateFunction {
   float evaluate2(Measurement a, Measurement b);

   float getUnacceptableRangeDistance();
}
