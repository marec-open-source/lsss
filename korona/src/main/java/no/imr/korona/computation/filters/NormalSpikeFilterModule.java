package no.imr.korona.computation.filters;

public final class NormalSpikeFilterModule extends SpikeFilterModule {
   public NormalSpikeFilterModule() {
   }

   @Override
   protected boolean isCenterValueDifferent(float centerData, float median) {
      return centerData > median + totalDelta.getFloatValue();
   }

   @Override
   protected float computeTestValue(float centerVerticalMedian) {
      return centerVerticalMedian - verticalDelta.getFloatValue();
   }

   @Override
   protected boolean spikeTest(float testValue, float verticalMedian) {
      return verticalMedian > testValue;
   }
}
