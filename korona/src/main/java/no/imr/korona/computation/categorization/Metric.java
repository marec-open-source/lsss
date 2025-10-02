package no.imr.korona.computation.categorization;

import no.imr.korona.computation.feature.Feature;

import java.util.function.ToDoubleBiFunction;

/**
 * For calculating the distance between two feature vectors.
 */
public enum Metric {
   /**
    * Implements d = sqrt( sum (x<sub>i</sub> - y<sub>i</sub>)<sup>2</sup> ).
    */
   EUCLIDEAN("euclidean", (pixel, neighbor) -> {
      double d = 0;
      for (Feature feature : pixel.getFeatures()) {
         Feature neighborFeature = neighbor.getFeature(feature.name());
         if (neighborFeature != null) {
            float x = feature.value() - neighborFeature.value();
            d += x * x;
         }
      }
      return Math.sqrt(d);
   }),

   /**
    * Implements d = sum (x<sub>i</sub> - y<sub>i</sub>)<sup>2</sup>.
    */
   EUCLIDEAN2("euclidean2", (pixel, neighbor) -> {
      double d = 0;
      for (Feature feature : pixel.getFeatures()) {
         Feature neighborFeature = neighbor.getFeature(feature.name());
         if (neighborFeature != null) {
            float x = feature.value() - neighborFeature.value();
            d += x * x;
         }
      }
      return d;
   }),

   /**
    * Implements d = max |x<sub>i</sub> - y<sub>i</sub>|.
    */
   MAX("mMax", (pixel, neighbor) -> {
      float d = 0;
      for (Feature feature : pixel.getFeatures()) {
         Feature neighborFeature = neighbor.getFeature(feature.name());
         if (neighborFeature != null) {
            float x = Math.abs(feature.value() - neighborFeature.value());
            d = Math.max(d, x);
         }
      }
      return d;
   });

   /**
    * Returns the distance between the feature vectors in a Pixel and a Neighbor.
    *
    * @param pixel    a pixel
    * @param neighbor a Neighbor
    * @return the distance between them
    */
   public float distance(Pixel pixel, Neighbor neighbor) {
      return (float) distanceFunction.applyAsDouble(pixel, neighbor);
   }

   private final String id;
   private final ToDoubleBiFunction<Pixel, Neighbor> distanceFunction;

   Metric(String id, ToDoubleBiFunction<Pixel, Neighbor> distanceFunction) {
      this.id = id;
      this.distanceFunction = distanceFunction;
   }

   @Override
   public String toString() {
      return id;
   }
}
