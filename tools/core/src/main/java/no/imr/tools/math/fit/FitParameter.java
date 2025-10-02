package no.imr.tools.math.fit;

/**
 * A parameter to be held by {@link FitFunction}.
 * <p>
 * The class contains:
 * <ul>
 * <li>A value</li>
 * <li>A step size for the parameter</li>
 * <li>The next value from the iteration (not necessarily the same as current + step)</li>
 * </ul>
 */
public final class FitParameter {
   private double value;
   private double stepSize;
   private double nextValue;

   public FitParameter(double value, double stepSize) {
      this.value = value;
      this.stepSize = stepSize;
   }

   public double getValue() {
      return value;
   }

   public void setValue(double value) {
      this.value = value;
   }

   public double getStepSize() {
      return stepSize;
   }

   public void setStepSize(double stepSize) {
      this.stepSize = stepSize;
   }

   public void stepUp() {
      value += stepSize;
   }

   public void stepDown() {
      value -= stepSize;
   }

   public void setNextValue(double nextValue) {
      this.nextValue = nextValue;
   }

   public void goToNextValue() {
      value = nextValue;
   }
}
