package no.imr.tools.math.irls;

public final class WeightedObservation {
   private final Observation observation;
   private double weight;
   private double residual;

   WeightedObservation(Observation observation) {
      this.observation = observation;
      weight = 1;
      residual = 0;
   }

   public Observation getObservation() {
      return observation;
   }

   public double getWeight() {
      return weight;
   }

   public void setWeight(double weight) {
      this.weight = weight;
   }

   public double getResidual() {
      return residual;
   }

   public void setResidual(double residual) {
      this.residual = residual;
   }
}
