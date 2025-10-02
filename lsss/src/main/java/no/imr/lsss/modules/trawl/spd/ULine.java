package no.imr.lsss.modules.trawl.spd;

public final class ULine extends BaseLine {
   final char interval;
   public final char sex;
   public final int minLengthGroup;
   private final String lengthDistribution;

   ULine(String line) {
      super('U', line);

      interval = charAt(line, 40);
      sex = charAt(line, 41);
      minLengthGroup = Integer.parseInt(substring(line, 42, 45).trim());
      lengthDistribution = substring(line, 45, line.length());
   }

   public double getLengthIntervalCm() {
      return switch (interval) {
         case '1' -> 0.1;  // 1mm
         case '2' -> 0.5;  // 5mm
         case '3' -> 1;    // 1cm
         case '4' -> 3;    // 3cm
         case '5' -> 5;    // 5cm
         case '6' -> 0.05; // 0.5mm
         case '7' -> 0.01; // 0.1mm
         default -> 1;
      };
   }

   public double getCmPerLengthUnit() {
      return switch (interval) {
         case '1',   // 1mm
              '2'    // 5mm
               -> 0.1;

         case '3',   // 1cm
              '4',   // 3cm
              '5'    // 5cm
               -> 1;

         case '6',   // 0.5mm
              '7'    // 0.1mm
               -> 0.01;

         default -> 1;
      };
   }

   public String getLengthUnit() {
      return switch (interval) {
         case '1',   // 1mm
              '2'    // 5mm
               -> "mm";

         case '3',   // 1cm
              '4',   // 3cm
              '5'    // 5cm
               -> "cm";

         case '6',   // 0.5mm
              '7'    // 0.1mm
               -> "0.1mm";

         default -> "?";
      };
   }

   public int[] getFishCounts() {
      int[] counts = new int[lengthDistribution.length() / 2];
      for (int i = 0; i < counts.length; i++) {
         try {
            String count = lengthDistribution.substring(i * 2, i * 2 + 2).trim();
            counts[i] = Integer.parseInt(count);
         } catch (Exception e) {
            counts[i] = 0;
         }
      }
      return counts;
   }
}
