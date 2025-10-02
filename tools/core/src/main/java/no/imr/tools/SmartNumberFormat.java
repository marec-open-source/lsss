package no.imr.tools;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * A NumberFormat chooses between ordinary and scientific representation
 * depending on number magnitude.
 */
public final class SmartNumberFormat extends NumberFormat {
   private final NumberFormat noExp = Utils.createDecimalFormat("###.#######");
   private final NumberFormat exp1 = Utils.createDecimalFormat("##0.#######E0");
   private final NumberFormat exp2 = Utils.createDecimalFormat("##0.#######E00");

   public SmartNumberFormat() {
   }

   private NumberFormat getFormat(double number) {
      double v = Math.abs(number);
      if (v == 0 || v >= 1e-2 && v < 1e3) {
         return noExp;
      } else if (v >= 1e-9 && v < 1e10) {
         return exp1;
      } else {
         return exp2;
      }
   }

   @Override
   public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
      return getFormat(number).format(number, toAppendTo, pos);
   }

   @Override
   public StringBuffer format(long number,
                              StringBuffer toAppendTo,
                              FieldPosition pos) {
      return getFormat(number).format(number, toAppendTo, pos);
   }

   @Override
   public Number parse(String source, ParsePosition parsePosition) {
      return exp2.parse(source, parsePosition);
   }
}
