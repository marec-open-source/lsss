package no.imr.tools.io;

import java.io.PrintWriter;

public final class Print {
   private Print() {
   }

   public static void commaAndValue(PrintWriter out, int value) {
      out.print(',');
      out.print(value);
   }

   public static void commaAndValue(PrintWriter out, String value) {
      out.print(',');
      out.print(value);
   }

   public static void leftPaddedValue(PrintWriter out, int width, int value) {
      leftPaddedValue(out, width, Integer.toString(value));
   }

   public static void leftPaddedValue(PrintWriter out, int width, String value) {
      for (int i = width - value.length(); i > 0; i--) {
         out.print(' ');
      }
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
      for (int i = width - value.length(); i > 0; i--) {
         out.print(' ');
      }
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
}
