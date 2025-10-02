package no.imr.lsss.modules.schoolparameter.perchannel;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.tools.math.Histogram1D;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.Map;

public final class HistogramParameterCollection implements PerChannelParameterCollection {
   private static final SchoolParameter TRUNCATED_MEAN_05 = new SchoolParameter(new Name("svMeanTruncated05", "sv mean (truncated 5%)"), Unit.SV);
   private static final SchoolParameter TRUNCATED_MEAN_10 = new SchoolParameter(new Name("svMeanTruncated10", "sv mean (truncated 10%)"), Unit.SV);
   private static final SchoolParameter TRUNCATED_MEAN_25 = new SchoolParameter(new Name("svMeanTruncated25", "sv mean (truncated 25%)"), Unit.SV);
   private static final SchoolParameter MEDIAN = new SchoolParameter(new Name("svMedian", "sv median"), Unit.SV);

   public HistogramParameterCollection() {
   }

   @Override
   public List<SchoolParameter> getParameters() {
      return List.of(
            TRUNCATED_MEAN_05,
            TRUNCATED_MEAN_10,
            TRUNCATED_MEAN_25,
            MEDIAN
      );
   }

   @Override
   public PerChannelComputer createComputer() {
      return new Computer();
   }

   private static final class Computer implements PerChannelComputer {
      private final Histogram1D histogram1D = Histogram1D.fromDelta(FloatRange.of(-150, 0), 0.01f);

      private Computer() {
      }

      @Override
      public void accumulate(Ping ping, double pingWidthMeters, int channel, List<FloatRange> depthRanges) {
         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            return;
         }
         float[] logSvArray = powerData.getLogSv();
         for (FloatRange depthRange : depthRanges) {
            int iBegin = powerData.depthToClampedSampleIndex(depthRange.min());
            int iEnd = powerData.depthToClampedSampleIndex(depthRange.max());
            for (int iSv = iBegin; iSv < iEnd; iSv++) {
               float logSv = logSvArray[iSv];
               histogram1D.addValue(logSv);
            }
         }
      }

      @Override
      public Map<String, Float> getValues() {
         return Map.of(
               TRUNCATED_MEAN_05.getPersistentName(), HistogramTruncatedMean.getTruncatedMean(histogram1D, 0.05f),
               TRUNCATED_MEAN_10.getPersistentName(), HistogramTruncatedMean.getTruncatedMean(histogram1D, 0.1f),
               TRUNCATED_MEAN_25.getPersistentName(), HistogramTruncatedMean.getTruncatedMean(histogram1D, 0.25f),
               MEDIAN.getPersistentName(), HistogramMedian.getMedian(histogram1D)
         );
      }
   }
}
