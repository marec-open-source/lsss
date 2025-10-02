package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;

final class DownsamplingModuleComputation extends ConcurrentPingModuleComputation {
   private final DownsamplingModule module;

   DownsamplingModuleComputation(DownsamplingModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      this.module = module;
   }

   @Override
   protected void processPing(Ping ping) {
      ping.getNonNullPowerDatas().forEach(this::downsample);
   }

   private void downsample(PowerData powerData) {
      int downsamplingFactor = getDownsamplingFactor(powerData.getSampleDistance());

      float[] originalSv = powerData.getSv();
      AngleData angleData = powerData.getAngleData();

      float[] downsampledSv = new float[originalSv.length / downsamplingFactor];
      for (int i = 0; i < downsampledSv.length; i++) {
         double svSum = 0;
         for (int j = 0; j < downsamplingFactor; j++) {
            int originalIndex = i * downsamplingFactor + j;
            svSum += originalSv[originalIndex];
         }
         downsampledSv[i] = (float) (svSum / downsamplingFactor);
      }
      powerData.setSv(downsampledSv);
      powerData.setSampleDistance(powerData.getSampleDistance() * downsamplingFactor);
      powerData.setOffset(powerData.getOffset() / downsamplingFactor);

      if (angleData != null) {
         float[] originalAngles = angleData.getElectricalAngles();
         float[] downsampledAngles = new float[2 * powerData.getCount()];
         for (int i = 0; i < powerData.getCount(); i++) {
            int originalIndex = i * downsamplingFactor + (downsamplingFactor - 1) / 2; // center of downsample interval
            downsampledAngles[2 * i] = originalAngles[2 * originalIndex];
            downsampledAngles[2 * i + 1] = originalAngles[2 * originalIndex + 1];
         }
         powerData.setElectricAngles(downsampledAngles);
      }
   }

   private int getDownsamplingFactor(float originalSampleSize) {
      return switch (module.downsamplingMethod.getValue()) {
         case NONE -> 1;
         case FACTOR -> module.downsamplingFactor.getIntValue();
         case SAMPLE_SIZE -> Math.max(1, Math.round(module.downsamplingSampleSize.getFloatValue() / originalSampleSize));
      };
   }
}
