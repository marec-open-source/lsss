package no.imr.lsss.modules.schoolparameter.perchannel;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;
import org.apache.commons.statistics.descriptive.Kurtosis;
import org.apache.commons.statistics.descriptive.Mean;
import org.apache.commons.statistics.descriptive.Skewness;
import org.apache.commons.statistics.descriptive.Variance;

import java.util.List;
import java.util.Map;

public final class MomentsParameterCollection implements PerChannelParameterCollection {
   private static final SchoolParameter SAMPLE_COUNT = new SchoolParameter(new Name("sampleCount", "Sample count"), Unit.COUNT);
   private static final SchoolParameter MIN = new SchoolParameter(new Name("svMin", "sv min"), Unit.SV);
   private static final SchoolParameter MAX = new SchoolParameter(new Name("svMax", "sv max"), Unit.SV);
   private static final SchoolParameter MEAN = new SchoolParameter(new Name("svMean", "sv mean"), Unit.SV);
   private static final SchoolParameter VARIANCE = new SchoolParameter(new Name("svVariance", "sv variance"), Unit.NONE);
   private static final SchoolParameter SKEWNESS = new SchoolParameter(new Name("svSkewness", "sv skewness"), Unit.NONE);
   private static final SchoolParameter KURTOSIS = new SchoolParameter(new Name("svKurtosis", "sv kurtosis"), Unit.NONE);

   public MomentsParameterCollection() {
   }

   @Override
   public List<SchoolParameter> getParameters() {
      return List.of(
            SAMPLE_COUNT,
            MIN,
            MAX,
            MEAN,
            VARIANCE,
            SKEWNESS,
            KURTOSIS
      );
   }

   @Override
   public PerChannelComputer createComputer() {
      return new Computer();
   }

   private static final class Computer implements PerChannelComputer {
      private long sampleCount;
      private float min = Float.POSITIVE_INFINITY;
      private float max = Float.NEGATIVE_INFINITY;
      private final Mean mean = Mean.create();
      private final Variance variance = Variance.create();
      private final Skewness skewness = Skewness.create();
      private final Kurtosis kurtosis = Kurtosis.create();

      private Computer() {
      }

      @Override
      public void accumulate(Ping ping, double pingWidthMeters, int channel, List<FloatRange> depthRanges) {
         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            return;
         }
         float[] sv = powerData.getSv();
         for (FloatRange depthRange : depthRanges) {
            int iBegin = powerData.depthToClampedSampleIndex(depthRange.min());
            int iEnd = powerData.depthToClampedSampleIndex(depthRange.max());
            sampleCount += iEnd - iBegin;
            for (int i = iBegin; i < iEnd; i++) {
               float value = sv[i];
               if (value < min) {
                  min = value;
               }
               if (value > max) {
                  max = value;
               }
               mean.accept(value);
               variance.accept(value);
               skewness.accept(value);
               kurtosis.accept(value);
            }
         }
      }

      @Override
      public Map<String, Float> getValues() {
         return Map.of(
               SAMPLE_COUNT.getPersistentName(), (float) sampleCount,
               MIN.getPersistentName(), min == Float.POSITIVE_INFINITY ? Float.NaN : min,
               MAX.getPersistentName(), max == Float.NEGATIVE_INFINITY ? Float.NaN : max,
               MEAN.getPersistentName(), (float) mean.getAsDouble(),
               VARIANCE.getPersistentName(), (float) variance.getAsDouble(),
               SKEWNESS.getPersistentName(), (float) skewness.getAsDouble(),
               KURTOSIS.getPersistentName(), (float) kurtosis.getAsDouble()
         );
      }
   }
}
