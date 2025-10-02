package no.imr.korona.computation.filters;

/**
 * A median-based spike filter.
 */
public final class BubbleSpikeFilterModule extends SpikeFilterModule {
   public BubbleSpikeFilterModule() {
      verticalMedianSearchHeight.setIntValue(50);
      verticalMedianSearchDuration.setFloatValue(2.0f);
      verticalMedianSearchDistance.setFloatValue(1.9f);
   }

   @Override
   protected boolean isCenterValueDifferent(float centerData, float median) {
      return centerData < median - totalDelta.getFloatValue();
   }

   @Override
   protected float computeTestValue(float centerVerticalMedian) {
      return centerVerticalMedian + verticalDelta.getFloatValue();
   }

   @Override
   protected boolean spikeTest(float testValue, float verticalMedian) {
      return verticalMedian < testValue;
   }
}
