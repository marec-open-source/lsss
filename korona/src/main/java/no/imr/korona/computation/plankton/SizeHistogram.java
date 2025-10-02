package no.imr.korona.computation.plankton;

import no.imr.tools.Utils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Size distribution and abundances represented as a histogram.
 */
public final class SizeHistogram {
   private static final String XML_SIZES = "sizes";

   private double[] dividers = Utils.EMPTY_DOUBLE_ARRAY;
   private double[] centers = Utils.EMPTY_DOUBLE_ARRAY;
   private double[] abundances = Utils.EMPTY_DOUBLE_ARRAY;

   public SizeHistogram() {
   }

   public SizeHistogram(double[] dividers) {
      copyDividers(dividers);
   }

   public SizeHistogram(Element element, double sizeFactor) throws PlanktonFileException {
      parseSizes(element.element(XML_SIZES), sizeFactor);
      if (!checkConsistencyOfHistogram()) {
         throw new PlanktonFileException("Histogram is corrupted, please check xml.");
      }
   }

   public void toXml(Element element, double sizeFactor) {
      Element histElement = DocumentHelper.createElement(XML_SIZES);
      StringBuilder sizes = new StringBuilder();
      for (int i = 0; i < abundances.length; i++) {
         sizes.append(Math.round(dividers[2 * i] / sizeFactor)).append(' ').append(Utils.toString(abundances[i])).append(" ; ");
      }
      if (dividers.length > 0) {
         sizes.append(Math.round(dividers[dividers.length - 1] / sizeFactor));
      }
      histElement.addText(sizes.toString());
      element.add(histElement);
   }

   private boolean parseSizes(@Nullable Element element, double sizeFactor) {
      if (element == null) {
         return false;
      }
      String[] words = element.getText().trim().split("[\\s;]+");
      resize((words.length - 1) / 2);
      for (int i = 0; i < words.length; i += 2) {
         double d = sizeFactor * Double.parseDouble(words[i]);
         if (i < dividers.length) {
            dividers[i] = d;
         }
         if (i > 0) {
            dividers[i - 1] = d;
         }
      }
      computeCenters();
      for (int i = 0; i < abundances.length; i++) {
         abundances[i] = Double.parseDouble(words[2 * i + 1]);
      }

      return checkConsistencyOfHistogram();
   }

   public void copyDividers(double[] newDividers) {
      if (newDividers.length % 2 != 0) {
         throw new IllegalArgumentException(Arrays.toString(newDividers));
      }

      resize(newDividers.length / 2);
      System.arraycopy(newDividers, 0, dividers, 0, newDividers.length);
      computeCenters();
   }

   private void resize(int cellCount) {
      if (centers.length != cellCount) {
         dividers = new double[2 * cellCount];
         centers = new double[cellCount];
         abundances = new double[cellCount];
      }
   }

   public double[] getDividers() {
      return dividers;
   }

   public double[] getCenters() {
      return centers;
   }

   public double[] getAbundances() {
      return abundances;
   }

   void computeCenters() {
      for (int i = 0; i < centers.length; i++) {
         centers[i] = (dividers[2 * i] + dividers[2 * i + 1]) / 2;
      }
   }

   /**
    * Checks if the histogram is internally consistent. This means checking if
    * the histogram dividers are increasing.
    *
    * @return true if the histogram is ok
    */
   public boolean checkConsistencyOfHistogram() {
      for (int i = 0; i < dividers.length; i = i + 2) {
         if (!checkDividerConsistency(i, dividers, dividers[i])) {
            return false;
         }
      }
      return true;
   }

   public static boolean checkDividerConsistency(int dividerIndex, double[] dividers, double theValue) {
      boolean lowOk = false;
      boolean highOk = false;

      if (dividerIndex > 1) {
         if (dividers[dividerIndex - 2] < theValue) {
            lowOk = true;
         }
      } else { // if element 0, lowOK is always true.
         lowOk = true;
      }

      if (dividerIndex < dividers.length - 2) {
         if (theValue < dividers[dividerIndex + 2]) {
            highOk = true;
         }
      } else if (dividerIndex == dividers.length - 2) { //next to last bin
         if (theValue < dividers[dividerIndex + 1]) {
            highOk = true;
         }
      } else if (dividerIndex == dividers.length - 1) { // Last divider bin.
         highOk = true;
         lowOk = theValue > dividers[dividerIndex - 1];
      }

      return highOk && lowOk;
   }

   /**
    * Distribute dividers evenly between start and stop.
    *
    * @param start lower histogram bin edge
    * @param stop  upper histogram bin edge
    */
   public void distributeDividers(double start, double stop) {
      double range = stop - start;
      double binSize = range / getAbundances().length;

      double currentValue = start;
      for (int i = 0; i < dividers.length; i++) {
         dividers[i] = currentValue;
         i++;
         currentValue += binSize;
         dividers[i] = currentValue;
      }
      computeCenters();

      Arrays.fill(abundances, 0);
   }

   public void adjustDividers(int dividerArrayIndex, double theValue) {
      //This needs to start in the divider and adjust values.

      int index = dividerArrayIndex;

      //If this is the first cell, we start iterating upwards
      index += 2;

      boolean above = false;
      double previousValue = theValue;

      //First do upwards check. If we accessed the last cell, this loop will not be entered due to the +2.
      while (index < dividers.length && !above) {
         if (dividers[index] <= previousValue) {
            dividers[index] = previousValue + 0.0001;
            previousValue = dividers[index];
            index += 2;
         } else {
            above = true;
         }
      }
      if (!above) {
         // if the value is not above after exit, it means we reached the end of the divider array
         // and need to set the last value as well.
         if (dividers[dividers.length - 1] <= previousValue) {
            dividers[dividers.length - 1] = previousValue + 0.0001;
         }
      }

      index = dividerArrayIndex;
      if (index == dividers.length - 1) {
         index--;
      } else {
         index -= 2;
      }

      boolean below = false;
      previousValue = theValue;

      while (index >= 0 && !below) {
         if (dividers[index] >= previousValue) {
            dividers[index] = previousValue - 0.0001;
            previousValue = dividers[index];
            index -= 2;
         } else {
            below = true;
         }
      }

      dividers[dividerArrayIndex] = theValue;
      if (dividerArrayIndex > 1 && dividerArrayIndex != dividers.length - 1) {
         dividers[dividerArrayIndex - 1] = theValue;
      }
   }
}
