package no.imr.korona.viewer;

import no.imr.korona.viewer.coloring.ValueColor;
import no.imr.tools.Max;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.range.FloatRange;

import java.util.Arrays;

/**
 * Resamples echogram data to a specified depth range.
 */
public final class Resampler {
   private final int outputBegin;
   private final int outputEnd;
   private final float inputBegin;
   private final float deltaSample;

   private Resampler(int inputLength, FloatRange inputRange, int outputLength, FloatRange outputRange) {
      FloatRange intersection = outputRange.intersection(inputRange);

      if (intersection.isEmpty()) {
         inputBegin = 0;
         outputBegin = 0;
         outputEnd = 0;
         deltaSample = 0;
      } else {
         float yFactor = outputLength / outputRange.getSize();
         outputBegin = Math.round(yFactor * (intersection.min() - outputRange.min()));
         outputEnd = Math.round(yFactor * (intersection.max() - outputRange.min()));

         float indexFactor = inputLength / inputRange.getSize();
         inputBegin = indexFactor * (intersection.min() - inputRange.min());
         float inputEnd = indexFactor * (intersection.max() - inputRange.min());

         deltaSample = (inputEnd - inputBegin) / (float) (outputEnd - outputBegin);
      }
   }

   /**
    * Resample byte data into an array, setting "NULL data" {@link ValueColor#NO_DATA_BYTE} where no data exists.
    *
    * @param input        data to read from
    * @param inputRange   depth range of input
    * @param output       buffer to write to
    * @param outputRange  depth range of output
    * @param resampleMode resample mode
    */
   public static void sampleByteData(byte[] input, FloatRange inputRange,
                                     byte[] output, FloatRange outputRange,
                                     ResampleMode resampleMode) {
      Resampler resampler = new Resampler(input.length, inputRange, output.length, outputRange);
      resampler.sampleByte(input, output, resampleMode);
   }

   /**
    * Resample float data into an array, setting "NULL data" {@link ValueColor#NO_DATA_FLOAT} where no data exists.
    *
    * @param input        data to read from
    * @param inputRange   depth range of input
    * @param output       buffer to write to
    * @param outputRange  depth range of output
    * @param resampleMode resample mode
    */
   public static void sampleFloatData(float[] input, FloatRange inputRange,
                                      float[] output, FloatRange outputRange,
                                      ResampleMode resampleMode) {
      Resampler resampler = new Resampler(input.length, inputRange, output.length, outputRange);
      resampler.sampleFloat(input, output, resampleMode);
   }

   private void sampleByte(byte[] input, byte[] output, ResampleMode resampleMode) {
      if (outputBegin > 0) {
         Arrays.fill(output, 0, outputBegin, ValueColor.NO_DATA_BYTE);
      }

      if (deltaSample > 1) {
         switch (resampleMode) {
            case NEAREST -> sampleNearByte(input, output);
            case AVERAGE -> sampleAverageByte(input, output);
            case MAX -> sampleMaxByte(input, output);
         }
      } else {
         sampleNearByte(input, output);
      }

      if (outputEnd < output.length) {
         Arrays.fill(output, outputEnd, output.length, ValueColor.NO_DATA_BYTE);
      }
   }

   private void sampleFloat(float[] input, float[] output, ResampleMode resampleMode) {
      if (outputBegin > 0) {
         Arrays.fill(output, 0, outputBegin, ValueColor.NO_DATA_FLOAT);
      }

      if (deltaSample > 1) {
         switch (resampleMode) {
            case NEAREST -> sampleNearFloat(input, output);
            case AVERAGE -> sampleAverageFloat(input, output);
            case MAX -> sampleMaxFloat(input, output);
         }
      } else {
         sampleNearFloat(input, output);
      }

      if (outputEnd < output.length) {
         Arrays.fill(output, outputEnd, output.length, ValueColor.NO_DATA_FLOAT);
      }
   }

   private void sampleNearByte(byte[] input, byte[] output) {
      for (int i = outputBegin, n = 0; i < outputEnd; i++, n++) {
         int j = (int) (inputBegin + n * deltaSample);
         output[i] = input[j];
      }
   }

   private void sampleAverageByte(byte[] input, byte[] output) {
      for (int i = outputBegin, n = 0; i < outputEnd; i++, n++) {
         int beginJ = (int) (inputBegin + n * deltaSample);
         int endJ = (int) (inputBegin + (n + 1) * deltaSample);
         output[i] = (byte) Math.round(ArrayMath.mean(input, beginJ, endJ));
      }
   }

   private void sampleMaxByte(byte[] input, byte[] output) {
      for (int i = outputBegin, n = 0; i < outputEnd; i++, n++) {
         int beginJ = (int) (inputBegin + n * deltaSample);
         int endJ = (int) (inputBegin + (n + 1) * deltaSample);
         output[i] = Max.of(input, beginJ, endJ);
      }
   }

   private void sampleNearFloat(float[] input, float[] output) {
      for (int i = outputBegin, n = 0; i < outputEnd; i++, n++) {
         int j = (int) (inputBegin + n * deltaSample);
         output[i] = input[j];
      }
   }

   private void sampleAverageFloat(float[] input, float[] output) {
      for (int i = outputBegin, n = 0; i < outputEnd; i++, n++) {
         int beginJ = (int) (inputBegin + n * deltaSample);
         int endJ = (int) (inputBegin + (n + 1) * deltaSample);
         output[i] = ArrayMath.mean(input, beginJ, endJ);
      }
   }

   private void sampleMaxFloat(float[] input, float[] output) {
      for (int i = outputBegin, n = 0; i < outputEnd; i++, n++) {
         int beginJ = (int) (inputBegin + n * deltaSample);
         int endJ = (int) (inputBegin + (n + 1) * deltaSample);
         output[i] = Max.of(input, beginJ, endJ);
      }
   }
}
