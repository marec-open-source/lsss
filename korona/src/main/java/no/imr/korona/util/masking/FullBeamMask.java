package no.imr.korona.util.masking;

import no.imr.tools.range.IntRangeSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FullBeamMask {
   // Byte enum instead of java enum to save space
   private static final byte UNSEEN = 0;
   private static final byte PENDING = 1;
   private static final byte OUTSIDE = 2;
   private static final byte INSIDE = 3;

   private final byte[] mask;
   private int minInsideIndex = Integer.MAX_VALUE;
   private int maxInsideIndex = Integer.MIN_VALUE;

   FullBeamMask(int size) {
      mask = new byte[size];
   }

   FullBeamMask(int size, IntRangeSet compactBeamMask) {
      this(size);

      int[] indexes = compactBeamMask.getIndexes();
      if (indexes.length > 0) {
         minInsideIndex = indexes[0];
         maxInsideIndex = indexes[indexes.length - 1] - 1;
         for (int i = 0; i < indexes.length; ) {
            int beginIndex = indexes[i++];
            int endIndex = indexes[i++];
            Arrays.fill(mask, beginIndex, endIndex, INSIDE);
         }
      }
   }

   FullBeamMask(FullBeamMask fullBeamMask) {
      mask = fullBeamMask.mask.clone();
      minInsideIndex = fullBeamMask.minInsideIndex;
      maxInsideIndex = fullBeamMask.maxInsideIndex;
   }

   public void replaceValues(FullBeamMask fullBeamMask) {
      if (mask.length != fullBeamMask.mask.length) {
         throw new IllegalArgumentException(mask.length + " != " + fullBeamMask.mask.length);
      }
      System.arraycopy(fullBeamMask.mask, 0, mask, 0, mask.length);
      minInsideIndex = fullBeamMask.minInsideIndex;
      maxInsideIndex = fullBeamMask.maxInsideIndex;
   }

   public int size() {
      return mask.length;
   }

   public boolean isUnseen(int index) {
      return mask[index] == UNSEEN;
   }

   public boolean isInside(int index) {
      return mask[index] == INSIDE;
   }

   public boolean isAnyInside(int beginIndex, int endIndex) {
      for (int i = beginIndex; i < endIndex; i++) {
         if (mask[i] == INSIDE) {
            return true;
         }
      }
      return false;
   }

   public boolean isAllInside(int beginIndex, int endIndex) {
      for (int i = beginIndex; i < endIndex; i++) {
         if (mask[i] != INSIDE) {
            return false;
         }
      }
      return true;
   }

   public void setPending(int index) {
      mask[index] = PENDING;
   }

   public void setInside(int index) {
      mask[index] = INSIDE;
      if (minInsideIndex > index) {
         minInsideIndex = index;
      }
      if (maxInsideIndex < index) {
         maxInsideIndex = index;
      }
   }

   public void setOutside(int index) {
      mask[index] = OUTSIDE;
   }

   IntRangeSet toCompactBeamMask() {
      if (minInsideIndex > maxInsideIndex) {
         return new IntRangeSet();
      }
      List<Integer> indexes = new ArrayList<>();
      int i = minInsideIndex;
      while (true) {
         while (i <= maxInsideIndex && mask[i] != INSIDE) {
            i++;
         }
         if (i > maxInsideIndex) {
            break;
         }
         indexes.add(i); // i is inside: Add begin index
         i++;
         while (i < mask.length && mask[i] == INSIDE) {
            i++;
         }
         indexes.add(i); // i is outside: Add end index
      }
      return new IntRangeSet(indexes);
   }
}
