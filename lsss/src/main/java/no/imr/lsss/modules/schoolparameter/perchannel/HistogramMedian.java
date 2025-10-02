package no.imr.lsss.modules.schoolparameter.perchannel;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.math.Histogram1D;

final class HistogramMedian {
   private HistogramMedian() {
   }

   static float computeLinearBinValue(Histogram1D histogram1D, int bin) {
      float a = PowerData.logSvToSv((float) histogram1D.indexToValue(bin));
      float b = PowerData.logSvToSv((float) histogram1D.indexToValue(bin + 1));
      return (a + b) / 2;
   }

   static float getMedian(Histogram1D histogram1D) {
      int medianBin = histogram1D.getLowerQuantileIndex(0.5);
      return computeLinearBinValue(histogram1D, medianBin);
   }
}
