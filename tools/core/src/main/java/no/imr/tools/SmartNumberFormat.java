package no.imr.tools;

import org.jspecify.annotations.Nullable;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * A NumberFormat chooses between ordinary and scientific representation
 * depending on number magnitude.
 */
public final class SmartNumberFormat extends NumberFormat {
   private final NumberFormat noExp = Utils.createDecimalFormat("###.#######");
   private final NumberFormat withExp = Utils.createDecimalFormat("##0.#######E0");

   public SmartNumberFormat() {
   }

   private NumberFormat getFormat(double number) {
      double v = Math.abs(number);
      if (v == 0 || v >= 1e-2 && v < 1e3) {
         return noExp;
      } else {
         return withExp;
      }
   }

   public String formatToString(double number) {
      // NumberFormat.format is final and has a fast path for package java.text.
      return getFormat(number).format(number);
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
   public @Nullable Number parse(String source, ParsePosition parsePosition) {
      return noExp.parse(source, parsePosition);
   }
}
