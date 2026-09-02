package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.GateFunction;
import no.imr.korona.computation.tracking.data.Measurement;

public final class SimpleGateFunction implements GateFunction {
   private final float rangeScale;
   private final float alongScale;
   private final float athwartScale;
   private final float tsScale;

   public SimpleGateFunction(float rangeScale, float alongScale, float athwartScale, float tsScale) {
      this.rangeScale = rangeScale;
      this.alongScale = alongScale;
      this.athwartScale = athwartScale;
      this.tsScale = tsScale;
   }

   @Override
   public float evaluate2(Measurement a, Measurement b) {
      float dRange = (a.range() - b.range()) / rangeScale;
      float dAlong = (a.alongshipAngleRad() - b.alongshipAngleRad()) / alongScale;
      float dAthwart = (a.athwartshipAngleRad() - b.athwartshipAngleRad()) / athwartScale;
      float dTS = (a.tsc() - b.tsc()) / tsScale;

      return dRange * dRange + dAlong * dAlong + dAthwart * dAthwart + dTS * dTS;
   }

   @Override
   public float getUnacceptableRangeDistance() {
      return rangeScale;
   }
}
