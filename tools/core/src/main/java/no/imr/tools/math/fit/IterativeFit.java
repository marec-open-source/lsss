package no.imr.tools.math.fit;

import java.util.Collection;

/**
 * Class for performing an iterative fit.
 */
public final class IterativeFit<P> {
   private final FitFunction<P> fitFunction;
   private final Collection<P> fitDataPoints;

   private static final double SCALE_FACTOR = 0.9;

   /**
    * Create a new IterativeFit.
    *
    * @param fitFunction   the function to be fitted
    * @param fitDataPoints the set of data points to fit the function to
    */
   public IterativeFit(FitFunction<P> fitFunction, Collection<P> fitDataPoints) {
      this.fitFunction = fitFunction;
      this.fitDataPoints = fitDataPoints;
   }

   /**
    * Performs the actual fit for a given number of iterations.
    *
    * @return the RMS of the fit
    */
   public double doFit() {
      double rms = 0;
      double scale = 1.0;

      while (scale > 0.01) {
         rms = doIteration();
         scale *= SCALE_FACTOR;
      }
      return rms;
   }

   /**
    * Performs a single iteration. Steps all parameters up/down
    * and tries to minimize the RMS.
    *
    * @return the RMS
    */
   private double doIteration() {
      double currentRMS = getRMS();

      for (FitParameter par : fitFunction.getParameters()) {
         par.setStepSize(par.getStepSize() * SCALE_FACTOR);

         par.stepUp();
         double stepUpRMS = getRMS();

         par.stepDown();
         par.stepDown();
         double stepDownRMS = getRMS();

         par.stepUp();

         if (Math.min(stepUpRMS, stepDownRMS) < currentRMS) {
            if (stepDownRMS <= stepUpRMS) {
               par.setNextValue(par.getValue() - par.getStepSize());
            } else {
               par.setNextValue(par.getValue() + par.getStepSize());
            }
            continue;
         }

         double dStepUpDown = stepDownRMS - stepUpRMS;
         double dUpDownMinusCurrent = stepUpRMS + stepDownRMS - 2 * currentRMS;

         double ratio;
         if (Math.abs(dUpDownMinusCurrent) > 0.001 * Math.abs(currentRMS)) {
            ratio = Math.clamp(dStepUpDown / dUpDownMinusCurrent / 2, -1, 1);
         } else {
            ratio = 0;
         }
         par.setNextValue(par.getValue() + ratio * par.getStepSize());
      }
      for (FitParameter par : fitFunction.getParameters()) {
         par.goToNextValue();
      }

      return getRMS();
   }

   /**
    * {@return the RMS for the current state of FitFunction}
    */
   private double getRMS() {
      double sumOfSquares = 0;
      for (P dataPoint : fitDataPoints) {
         double error = fitFunction.fittedValue(dataPoint) - fitFunction.actualValue(dataPoint);
         sumOfSquares += error * error;
      }
      return Math.sqrt(sumOfSquares / fitDataPoints.size());
   }
}
