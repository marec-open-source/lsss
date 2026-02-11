package no.imr.lsss.modules.trawl.spd;

abstract class BaseLine {
   BaseLine(char type, String line) {
      if (charAt(line, 0) != type) {
         throw new IllegalArgumentException("Invalid " + type + "-line string: " + line);
      }
   }

   static char charAt(String string, int index) {
      if (index < string.length()) {
         return string.charAt(index);
      } else {
         return ' ';
      }
   }

   static String substring(String string, int beginIndex, int endIndex) {
      int end = Math.min(endIndex, string.length());
      if (beginIndex < end) {
         return string.substring(beginIndex, end);
      } else {
         return "";
      }
   }

   static int parseInt(String string) {
      return Integer.parseInt(string.trim());
   }

   static int parseInt(String string, int defaultValue) {
      try {
         return parseInt(string);
      } catch (Exception _) {
         return defaultValue;
      }
   }

   static double parseDouble(String string, double defaultValue) {
      try {
         return Double.parseDouble(string.trim());
      } catch (Exception _) {
         return defaultValue;
      }
   }
}
