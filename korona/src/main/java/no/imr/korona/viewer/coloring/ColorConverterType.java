package no.imr.korona.viewer.coloring;

public enum ColorConverterType {
   CONTINUOUS(true, false),
   DISCRETE(false, true),
   DISCRETE_LIGHT(true, true),
   DISCRETE_THRESHOLD(true, true);

   private final boolean continuous;
   private final boolean discrete;

   ColorConverterType(boolean continuous, boolean discrete) {
      this.continuous = continuous;
      this.discrete = discrete;
   }

   public boolean isContinuous() {
      return continuous;
   }

   public boolean isDiscrete() {
      return discrete;
   }
}
