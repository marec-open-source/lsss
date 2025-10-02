package no.imr.tools.math.fit;

import java.util.List;

/**
 * A datapoint used by IterativeFit.
 * A datapoint is here defined by a number of arguments and
 * a value analogous to the function to be fitted:
 * f(arguments) = value
 */
public interface FitDataPoint {
   List<Double> getArguments();

   double getValue();
}
