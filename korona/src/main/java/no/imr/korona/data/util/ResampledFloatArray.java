package no.imr.korona.data.util;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.Utils;
import no.imr.tools.math.MathUtils;

/**
 * Resample one data array from this sample datagram to the resolution of another.
 */
public record ResampledFloatArray(
      int offset,
      float[] values
) implements ResampledArray {
   @Override
   public int length() {
      return values.length;
   }

   public static ResampledFloatArray create(float[] valuesToBeResampled, PowerData powerDataToBeResampled, PowerData powerDataToResampleTo) {
      return resample(valuesToBeResampled, powerDataToBeResampled, powerDataToResampleTo, 0);
   }

   public static ResampledFloatArray create(float[] valuesToBeResampled, PowerData powerDataToBeResampled, PowerData powerDataToResampleTo,
                                            int startSampleOffset) {
      return resample(valuesToBeResampled, powerDataToBeResampled, powerDataToResampleTo, startSampleOffset);
   }

   private static ResampledFloatArray resample(float[] values, PowerData powerDataToBeResampled, PowerData powerDataToResampleTo, int startSampleOffset) {
      if (powerDataToBeResampled.getOffset() == powerDataToResampleTo.getOffset() &&
            powerDataToBeResampled.getSampleDistance() == powerDataToResampleTo.getSampleDistance() &&
            powerDataToBeResampled.getHeaveCorrectedTransducerDepth() == powerDataToResampleTo.getHeaveCorrectedTransducerDepth() &&
            startSampleOffset == 0) {
         return new ResampledFloatArray(startSampleOffset, values);
      } else if (powerDataToResampleTo.getSampleDistance() != powerDataToBeResampled.getSampleDistance()
            || powerDataToResampleTo.getHeaveCorrectedTransducerDepth() != powerDataToBeResampled.getHeaveCorrectedTransducerDepth()) {
         return fullResample(values, powerDataToBeResampled, powerDataToResampleTo, startSampleOffset);
      } else {
         if (startSampleOffset != 0) {
            int resampledCount = powerDataToBeResampled.getCount() - startSampleOffset;
            if (resampledCount <= 0) {
               //cannot resample
               return new ResampledFloatArray(0, Utils.EMPTY_FLOAT_ARRAY);
            }
            int offset = powerDataToResampleTo.getOffset() - powerDataToBeResampled.getOffset() + startSampleOffset;
            float[] resampledValues = new float[resampledCount];
            System.arraycopy(values, startSampleOffset, resampledValues, 0, resampledCount);
            return new ResampledFloatArray(offset, resampledValues);
         } else {
            int offset = powerDataToResampleTo.getOffset() - powerDataToBeResampled.getOffset();
            return new ResampledFloatArray(offset, values);
         }
      }
   }

   private static ResampledFloatArray fullResample(float[] values, PowerData powerDataToBeResampled, PowerData powerDataToResampleTo, int startSampleOffset) {
      float resampleMinDepth = powerDataToBeResampled.getSampleDepth(startSampleOffset);
      float referenceMinDepth = powerDataToResampleTo.getMinDepth();
      float referenceSampleDistance = powerDataToResampleTo.getSampleDistance();
      int offset = (int) Math.ceil((referenceMinDepth - resampleMinDepth) / referenceSampleDistance);
      //ensure enough samples -> extrapolation for the last sample
      int resampledCount = (int) Math.ceil((powerDataToBeResampled.getMaxDepth() - powerDataToResampleTo.getSampleDepth(-offset)) / referenceSampleDistance);
      if (resampledCount <= 0 || values.length == 0) {
         //cannot resample
         return new ResampledFloatArray(0, Utils.EMPTY_FLOAT_ARRAY);
      }
      float[] resampledValues = new float[resampledCount];

      float originalSampleDistance = powerDataToBeResampled.getSampleDistance();
      for (int index = 0; index < resampledCount; index++) {
         float referenceDepth = referenceMinDepth + (index - offset) * referenceSampleDistance;
         float originalIndexFloat = (referenceDepth - resampleMinDepth) / originalSampleDistance + startSampleOffset;
         int originalIndex = (int) Math.floor(originalIndexFloat);
         if (originalIndex >= 0 && originalIndex + 1 < values.length) {
            float weight = originalIndexFloat - originalIndex;
            resampledValues[index] = (float) MathUtils.interpolate(values[originalIndex], values[originalIndex + 1], weight);
         } else {
            resampledValues[index] = values[Math.clamp(originalIndex, 0, values.length - 1)];
         }
      }
      return new ResampledFloatArray(offset, resampledValues);
   }

   /**
    * Returns the value for the given index in the datagram this array is sampled into.
    *
    * @param i index in the datagram sampled into
    * @return the value for the given index
    */
   public float getValueForReferenceIndex(int i) {
      return values[getValidResampledIndex(i)];
   }
}
