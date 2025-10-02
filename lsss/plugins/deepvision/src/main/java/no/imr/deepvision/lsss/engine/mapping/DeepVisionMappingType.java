package no.imr.deepvision.lsss.engine.mapping;

public enum DeepVisionMappingType {
   IDENTITY("Identity"), GEO("Geographical");

   private final String value;

   DeepVisionMappingType(String value) {
      this.value = value;
   }

   @Override
   public String toString() {
      return value;
   }
}
