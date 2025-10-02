package no.imr.korona.data.util;

import java.util.Objects;

public interface ResampledArray {
   /**
    * {@return the offset of the resampled values}
    */
   int offset();

   /**
    * {@return the length of the resampled values}
    */
   int length();

   default int referenceIndexToResampledIndex(int i) {
      return i + offset();
   }

   default int getValidResampledIndex(int i) {
      int resampledIndex = referenceIndexToResampledIndex(i);
      return Objects.checkIndex(resampledIndex, length());
   }

   /**
    * Returns the first index which has a valid resampled value in this resampled array.
    *
    * @return the first valid index, may be less than zero
    */
   default int getBeginReferenceIndex() {
      return -offset();
   }

   /**
    * Returns one index past the last index which has a valid resampled value in this resampled array.
    *
    * @return one past the last valid index
    */
   default int getEndReferenceIndex() {
      return length() - offset();
   }
}
