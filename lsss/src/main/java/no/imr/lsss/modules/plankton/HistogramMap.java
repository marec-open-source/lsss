package no.imr.lsss.modules.plankton;

import no.imr.korona.data.datagrams.Pic0Datagram;

import java.util.HashMap;
import java.util.Map;

/**
 * One histogram per plankton category.
 */
final class HistogramMap {
   private final Map<Pic0Datagram.PlanktonCategory, Histogram> histograms = new HashMap<>();

   HistogramMap() {
   }

   Map<Pic0Datagram.PlanktonCategory, Histogram> getHistograms() {
      return histograms;
   }

   Histogram getHistogram(Pic0Datagram.PlanktonCategory planktonCategory) {
      return histograms.computeIfAbsent(planktonCategory, k -> new Histogram());
   }

   void clear() {
      histograms.clear();
   }

   void accumulate(HistogramMap histogramMap, float weightFactor) {
      histogramMap.histograms.forEach((planktonCategory, histogram) -> {
         getHistogram(planktonCategory).accumulate(histogram, weightFactor);
      });
   }
}
