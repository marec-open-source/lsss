package no.imr.tools.io;

import java.io.PrintWriter;
import java.util.List;

public final class Print {
   private Print() {
   }

   public static void blanks(PrintWriter out, int count) {
      for (int i = 0; i < count; i++) {
         out.print(' ');
      }
   }

   public static void commaAndValue(PrintWriter out, int value) {
      out.print(',');
      out.print(value);
   }

   public static void commaAndValue(PrintWriter out, String value) {
      out.print(',');
      out.print(value);
   }

   public static void spaceAndValue(PrintWriter out, int value) {
      out.print(' ');
      out.print(value);
   }

   public static void spaceAndValue(PrintWriter out, float value) {
      out.print(' ');
      out.print(value);
   }

   public static void spaceAndValue(PrintWriter out, String value) {
      out.print(' ');
      out.print(value);
   }

   public static void leftPaddedValue(PrintWriter out, int width, int value) {
      leftPaddedValue(out, width, Integer.toString(value));
   }

   public static void leftPaddedValue(PrintWriter out, int width, String value) {
      blanks(out, width - value.length());
      out.print(value);
   }

   public static void spaceAndLeftPaddedValue(PrintWriter out, int width, int value) {
      out.print(' ');
      leftPaddedValue(out, width, value);
   }

   public static void spaceAndLeftPaddedValue(PrintWriter out, int width, String value) {
      out.print(' ');
      leftPaddedValue(out, width, value);
   }

   public static void rightPaddedValue(PrintWriter out, int width, String value) {
      out.print(value);
      blanks(out, width - value.length());
   }

   public static void rightPaddedValue(PrintWriter out, int width, int value) {
      rightPaddedValue(out, width, Integer.toString(value));
   }

   public static void tabAndValue(PrintWriter out, int value) {
      out.print('\t');
      out.print(value);
   }

   public static void tabAndValue(PrintWriter out, String value) {
      out.print('\t');
      out.print(value);
   }


   public static void line(PrintWriter out, List<String> values, char separator) {
      for (int i = 0; i < values.size(); i++) {
         if (i != 0) {
            out.print(separator);
         }
         out.print(values.get(i));
      }
      out.println();
   }

   public static void paddedLine(PrintWriter out, List<String> values, List<Integer> widths, boolean rightAligned) {
      assert values.size() == widths.size();

      int actualIndex = 0;
      int targetIndex = 0;

      for (int i = 0; i < values.size(); i++) {
         String value = values.get(i);
         targetIndex += widths.get(i);

         if (i != 0) {
            out.print(' ');
            actualIndex++;
            targetIndex++;
         }
         actualIndex += value.length();

         int blanks = Math.max(0, targetIndex - actualIndex);
         actualIndex += blanks;

         int blanksBefore = rightAligned ? blanks : 0;
         int blanksAfter = blanks - blanksBefore;

         blanks(out, blanksBefore);
         out.print(value);
         blanks(out, blanksAfter);
      }
      out.println();
   }
}
