package no.imr.tools.math.fit;

import java.util.List;

/**
 * A function that can be fitted by the class IterativeFit.
 * It can evaluate a function of an arbitrary number of arguments,
 * and it can return the list of parameters to be fitted.
 */
public interface FitFunction {
   double evaluate(List<Double> arg);

   List<FitParameter> getParameters();
}
