package no.imr.tools.math.fit;

import java.util.List;

/**
 * A function that can be fitted by the class IterativeFit.
 * It can evaluate a function of an arbitrary number of arguments,
 * and it can return the list of parameters to be fitted.
 */
public interface FitFunction<P> {
   double fittedValue(P dataPoint);

   double actualValue(P dataPoint);

   List<FitParameter> getParameters();
}
