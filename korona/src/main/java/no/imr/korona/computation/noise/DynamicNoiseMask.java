package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.math.MathUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * DynamicNoiseMask.
 * Picks noise values according to recommendations in the report
 * "Bergen echo integrator with focus on recent advances"
 */
final class DynamicNoiseMask extends BaseNoiseMask {
   private String caseText = "";

   /**
    * A short range limit.
    */
   private final float rangeLimitShort;

   /**
    * Use data between this limit and the bottom in case A p.10.
    * in "BEI with Focus on Recent Advances"
    */
   private final float rangeLimit;

   /**
    * Pick data in the interval [bottom + belowBottom, bottom*2].
    * belowBottom depends on the frequency.
    * Rolf Korneliussen's suggestions:
    * <pre>
    *  18000 Hz: 185 m
    *  38000 Hz: 120 m
    * 120000 Hz:  60 m
    * 200000 Hz:  40 m
    * </pre>
    */
   private final float belowBottom;

   private final float rangeLimitDeep;    //So large range that received data are "passive"

   /**
    * The following numbers are suggestions by Rolf Korneliussen based on EK500 measurements.
    */
   private static final List<RangeLimits> RANGE_LIMITS = List.of(
         new RangeLimits(12, 275, 400, 1000, 2000),
         new RangeLimits(18, 185, 250, 650, 1500),
         new RangeLimits(38, 140, 200, 550, 1500),
         new RangeLimits(70, 80, 150, 325, 750),
         new RangeLimits(120, 60, 100, 235, 500),
         new RangeLimits(200, 45, 70, 175, 450),
         new RangeLimits(333, 35, 40, 90, 200),
         new RangeLimits(555, 15, 30, 75, 150)
   );

   private static final RangeLimits ABOVE_HIGH_LIMIT = new RangeLimits(Integer.MAX_VALUE, 15, 25, 50, 100);

   DynamicNoiseMask(int khz) {
      //
      // Linear interpolation of the range limits if the input frequency is between the frequencies with defined ranges in RANGE_LIMITS.
      //
      RangeLimits interpolatedRangeLimits = getInterpolatedRangeLimits(khz);

      //
      // EK80 is more sensitive than EK500. A multiplication factor 1.5 seems to be reasonable,
      // but start with 1.25.
      //
      float ek = 1.25f;
      belowBottom = interpolatedRangeLimits.belowBottom * ek;
      rangeLimitShort = interpolatedRangeLimits.rangeLimitShort * ek;
      rangeLimit = interpolatedRangeLimits.rangeLimit * ek;
      rangeLimitDeep = interpolatedRangeLimits.rangeLimitDeep * ek;
   }

   private static RangeLimits getInterpolatedRangeLimits(int kHz) {
      int i = Utils.binarySearchForInt(RANGE_LIMITS, kHz, RangeLimits::kHz);
      if (i >= 0) {
         return RANGE_LIMITS.get(i);
      }
      int insertionPoint = -i - 1;
      if (insertionPoint == 0) {
         return RANGE_LIMITS.getFirst();
      }
      if (insertionPoint == RANGE_LIMITS.size()) {
         return new RangeLimits(kHz, ABOVE_HIGH_LIMIT.belowBottom, ABOVE_HIGH_LIMIT.rangeLimitShort, ABOVE_HIGH_LIMIT.rangeLimit, ABOVE_HIGH_LIMIT.rangeLimitDeep);
      }
      return RangeLimits.interpolate(kHz, RANGE_LIMITS.get(insertionPoint - 1), RANGE_LIMITS.get(insertionPoint));
   }

   private static @Nullable HistogramData intervalMask(HistogramData histogramData, Ping ping, float minRange, float maxRange, RangeValues rangeValues) {
      IntervalNoiseMask intervalMask = new IntervalNoiseMask(minRange, maxRange);
      return intervalMask.mask(histogramData, ping, rangeValues);
   }

   @Override
   @Nullable HistogramData mask(@Nullable HistogramData histogramData, Ping ping, RangeValues rangeValues) {
      if (histogramData == null || histogramData.isEmpty()) {
         return null;
      }
      HistogramData.Quality quality = histogramData.getQuality();
      HistogramData hist;

      //
      // Similar to passive recordings: Quality = 100
      //
      if (rangeValues.hasBottomRange()) {
         float bottom = rangeValues.getBelowFirstBottomRange();
         float minRange = bottom + belowBottom;
         float maxRange = 2 * rangeValues.getAboveFirstBottomRange() - 10; // 10m above 2nd bottom echo
         hist = intervalMask(histogramData, ping, minRange, maxRange, rangeValues);
         if (hist != null) {
            setCase("Between first and second bottom echo");
            quality.category = 1;
            quality.value = 100;
            return hist;
         }
      }

      hist = intervalMask(histogramData, ping, rangeLimitDeep, Float.POSITIVE_INFINITY, rangeValues);
      if (hist != null) {
         setCase("Below RangeLimitDeep");
         quality.category = 2;
         quality.value = 100;
         return hist;
      }

      // No bottom detected at any frequency
      if (!rangeValues.hasBottomRange()) {
         float maxDataRange = ping.getPingData().getDataRange();
         if (maxDataRange >= 500) {
            hist = intervalMask(histogramData, ping, rangeLimit, Float.POSITIVE_INFINITY, rangeValues);
            if (hist != null) {
               setCase("Below RangeLimit when no bottom is detected at any frequency");
               quality.category = 3;
               quality.value = 90;
               return hist;
            }
         }
      }

      // Quality 75
      {
         float reductionFactor = 0.75f;

         if (rangeValues.hasBottomRange()) {
            float bottom = rangeValues.getBelowFirstBottomRange();
            float minRange = bottom + belowBottom * reductionFactor;
            float maxRange = 2 * rangeValues.getAboveFirstBottomRange() - 10; // 10m above 2nd bottom echo
            hist = intervalMask(histogramData, ping, minRange, maxRange, rangeValues);
            if (hist != null) {
               setCase("Between first and second bottom echo (reduced)");
               quality.category = 4;
               quality.value = 75;
               return hist;
            }
         }

         float reducedRangeLimitDeep = rangeLimitDeep * reductionFactor;
         hist = intervalMask(histogramData, ping, reducedRangeLimitDeep, Float.POSITIVE_INFINITY, rangeValues);
         if (hist != null) {
            setCase("Below reduced RangeLimitDeep");
            quality.category = 5;
            quality.value = 75;
            return hist;
         }
      }

      if (rangeValues.hasBottomRange()) {
         float bottom = rangeValues.getAboveFirstBottomRange();
         hist = intervalMask(histogramData, ping, rangeLimit, bottom - 10, rangeValues);
         if (hist != null) {
            setCase("Between RangeLimit and bottom");
            quality.category = 6;
            float x = Math.clamp((bottom - 10 - rangeLimit) / (rangeLimitDeep - rangeLimit), 0, 1);
            quality.value = 50 + 25 * x;    //Somewhere between RangeLimit-quality (50) and RangeLimitDeep-quality (75)
            return hist;
         }
      }

      hist = intervalMask(histogramData, ping, rangeLimitShort, Float.POSITIVE_INFINITY, rangeValues);
      if (hist != null) {
         setCase("Below RangeLimit");
         quality.category = 7;
         quality.value = 25;
         return hist;
      }

      setCase("All data");
      quality.category = 8;
      quality.value = 0;
      return histogramData;
   }

   private void setCase(String text) {
      if (caseText.equals(text)) {
         return;
      }
      caseText = text;
      Log.global.finer(getLogLabel() + "Now using noise data: " + text);
   }

   record RangeLimits(
         int kHz,
         float belowBottom,
         float rangeLimitShort,
         float rangeLimit,
         float rangeLimitDeep
   ) {
      static RangeLimits interpolate(int targetKHz, RangeLimits rangeLimitsLow, RangeLimits rangeLimitsHigh) {
         double w = (double) (targetKHz - rangeLimitsLow.kHz) / (rangeLimitsHigh.kHz - rangeLimitsLow.kHz);
         return new RangeLimits(targetKHz,
               (float) MathUtils.interpolate(rangeLimitsLow.belowBottom, rangeLimitsHigh.belowBottom, w),
               (float) MathUtils.interpolate(rangeLimitsLow.rangeLimitShort, rangeLimitsHigh.rangeLimitShort, w),
               (float) MathUtils.interpolate(rangeLimitsLow.rangeLimit, rangeLimitsHigh.rangeLimit, w),
               (float) MathUtils.interpolate(rangeLimitsLow.rangeLimitDeep, rangeLimitsHigh.rangeLimitDeep, w)
         );
      }
   }
}
