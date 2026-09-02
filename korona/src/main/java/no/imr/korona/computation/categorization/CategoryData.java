package no.imr.korona.computation.categorization;

/**
 * Contains data used when categorizing a pixel.
 */
final class CategoryData implements Comparable<CategoryData> {
   private final Category category;
   private float discriminant;
   private float boundedProbability; //between 0 and 1 // todo: use integrated values....?
   private float normalizedProbability; //integral normalized to 1
   private float apriori;
   private float contextualApriori = 1;
   private float probabilityThreshold;

   /**
    * Constructor.
    *
    * @param category              the category
    * @param discriminant          the discriminant value
    * @param boundedProbability    probability between 0 and 1
    * @param normalizedProbability the value of the probability density function
    */
   CategoryData(Category category, float discriminant, float boundedProbability,
                float normalizedProbability, float probabilityThreshold) {
      this.category = category;
      this.discriminant = discriminant;
      this.boundedProbability = boundedProbability;
      this.normalizedProbability = normalizedProbability;
      this.probabilityThreshold = probabilityThreshold;

      apriori = category.getApriori();
   }

   @Override
   public String toString() {
      return category.getName();
   }

   /**
    * NB: CategoryData compares opposite to the numerical value of the discriminant.
    *
    * @param other the CategoryData to compare to
    * @return -1, 0, or +1 as this object is less than, equal to, or greater
    * than the specified object.
    */
   @Override
   public int compareTo(CategoryData other) {
      return Float.compare(other.discriminant, discriminant);
   }

   Category getCategory() {
      return category;
   }

   float getBoundedProbability() {
      return boundedProbability;
   }

   float getNormalizedProbability() {
      return normalizedProbability;
   }

   float getDiscriminant() {
      return discriminant;
   }

   void setDiscriminant(float discriminant) {
      this.discriminant = discriminant;
   }

   float getTotalApriori() {
      return apriori * contextualApriori;
   }

   float getApriori() {
      return apriori;
   }

   void setApriori(float apriori) {
      this.apriori = apriori;
   }

   float getContextualApriori() {
      return contextualApriori;
   }

   void setContextualApriori(float contextualApriori) {
      this.contextualApriori = contextualApriori;
   }

   boolean isAcceptable() {
      return getTotalApriori() * normalizedProbability > probabilityThreshold;
   }
}
