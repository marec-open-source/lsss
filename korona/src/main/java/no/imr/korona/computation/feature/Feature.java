package no.imr.korona.computation.feature;

/**
 * Name and value of a feature.
 */
public record Feature(String name, float value) {

   public Feature(String name, double value) {
      this(name, (float) value);
   }

   @Override
   public String toString() {
      return name + " = " + value;
   }
}
