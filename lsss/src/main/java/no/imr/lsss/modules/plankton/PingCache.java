package no.imr.lsss.modules.plankton;

import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.datagrams.Pid0Datagram;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.range.FloatRange;

import java.util.List;

/**
 * The combined histograms for the pixels from one ping in one region.
 */
final class PingCache {
   private final HistogramMap histogramMap = new HistogramMap();

   PingCache(List<FloatRange> depthRanges, Pic0Datagram pic0, Pid0Datagram pid0, PowerData powerData, FloatRange svRange) {
      List<Pid0Datagram.PlanktonSample> planktonSamples = pid0.getPlanktonSamples(pic0);
      float[] svArray = powerData.getSv();

      for (FloatRange depthRange : depthRanges) {
         int iMin = Math.clamp(pid0.depthToIndex(depthRange.min()), 0, planktonSamples.size());
         int iMax = Math.clamp(pid0.depthToIndex(depthRange.max()), 0, planktonSamples.size());
         for (int i = iMin; i < iMax; i++) {
            int svIndex = powerData.depthToSampleIndex(pid0.indexToDepth(i));
            if (svIndex < 0 || svIndex >= powerData.getCount()) {
               continue;
            }
            float sv = svArray[svIndex];
            if (!svRange.contains(sv)) {
               continue;
            }

            Pid0Datagram.PlanktonSample planktonSample = planktonSamples.get(i);
            Pid0Datagram.PlanktonData planktonData = planktonSample.getBestPlanktonData();
            Pic0Datagram.PlanktonCategory planktonCategory = planktonData.getPlanktonCategory();
            if (planktonCategory == null) {
               continue;
            }
            Histogram histogram = histogramMap.getHistogram(planktonCategory);
            Pid0Datagram.LengthDistribution lengthDistribution = planktonData.getLengthDistribution();
            histogram.accumulate(lengthDistribution.getDividers(), lengthDistribution.getAbundances(), pid0.getSampleDistance());
         }
      }
   }

   HistogramMap getHistogramMap() {
      return histogramMap;
   }
}
