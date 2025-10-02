package no.imr.korona.data.util;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.Utils;

public record ResampledBooleanArray(
      int offset,
      boolean[] values
) implements ResampledArray {
   @Override
   public int length() {
      return values.length;
   }

   public static ResampledBooleanArray create(boolean[] valuesToBeResampled, PowerData powerDataToBeResampled, PowerData powerDataToResampleTo) {
      return resample(valuesToBeResampled, powerDataToBeResampled, powerDataToResampleTo);
   }

   private static ResampledBooleanArray resample(boolean[] values, PowerData powerDataToBeResampled, PowerData powerDataToResampleTo) {
      if (powerDataToBeResampled.getOffset() == powerDataToResampleTo.getOffset() &&
            powerDataToBeResampled.getSampleDistance() == powerDataToResampleTo.getSampleDistance() &&
            powerDataToBeResampled.getHeaveCorrectedTransducerDepth() == powerDataToResampleTo.getHeaveCorrectedTransducerDepth()) {
         return new ResampledBooleanArray(0, values);
      } else if (powerDataToResampleTo.getSampleDistance() != powerDataToBeResampled.getSampleDistance()
            || powerDataToResampleTo.getHeaveCorrectedTransducerDepth() != powerDataToBeResampled.getHeaveCorrectedTransducerDepth()) {
         return fullResample(values, powerDataToBeResampled, powerDataToResampleTo, 0);
      } else {
         int offset = powerDataToResampleTo.getOffset() - powerDataToBeResampled.getOffset();
         return new ResampledBooleanArray(offset, values);
      }
   }

   private static ResampledBooleanArray fullResample(boolean[] values, PowerData powerDataToBeResampled, PowerData powerDataToResampleTo, int startSampleOffset) {
      float resampleMinDepth = powerDataToBeResampled.getSampleDepth(startSampleOffset);
      float referenceMinDepth = powerDataToResampleTo.getMinDepth();
      float referenceSampleDistance = powerDataToResampleTo.getSampleDistance();
      int offset = (int) Math.ceil((referenceMinDepth - resampleMinDepth) / referenceSampleDistance);
      //ensure enough samples -> extrapolation for the last sample
      int resampledCount = (int) Math.ceil((powerDataToBeResampled.getMaxDepth() - powerDataToResampleTo.getSampleDepth(-offset)) / referenceSampleDistance);
      if (resampledCount <= 0 || values.length == 0) {
         //cannot resample
         return new ResampledBooleanArray(0, Utils.EMPTY_BOOLEAN_ARRAY);
      }
      boolean[] resampledValues = new boolean[resampledCount];

      float originalSampleDistance = powerDataToBeResampled.getSampleDistance();
      for (int index = 0; index < resampledCount; index++) {
         float referenceDepth = referenceMinDepth + (index - offset) * referenceSampleDistance;
         float originalIndexFloat = (referenceDepth - resampleMinDepth) / originalSampleDistance + startSampleOffset;
         int originalIndex = (int) Math.floor(originalIndexFloat);
         if (originalIndex >= 0 && originalIndex + 1 < values.length) {
            float weight = originalIndexFloat - originalIndex;
            resampledValues[index] = weight < 0.5 ? values[originalIndex] : values[originalIndex + 1]; //nearest neighbour interpolation
         } else {
            resampledValues[index] = values[Math.clamp(originalIndex, 0, values.length - 1)];
         }
      }
      return new ResampledBooleanArray(offset, resampledValues);
   }
}
