package no.imr.lsss.modules.trawl.spd;

public final class TLine extends BaseLine {
   final char specieCode;
   public final String speciesName;
   public final int speciePartNo;
   final String sampleType;
   public final String group;
   final char conservation;
   final char measure;
   public final double catchQuantum;
   public final int catchNumber;
   final char target;
   final char lengthUnit;
   final String weightOfLengthSample;
   public final int lengthSampleNo;
   final String individualSampleNo;
   final char otolithShell;
   final char parasite;
   final char stomach;
   final char genetics;

   TLine(String line) {
      super('T', line);

      specieCode = charAt(line, 26);
      speciesName = substring(line, 27, 39);
      speciePartNo = parseInt(substring(line, 39, 40), 0);

      sampleType = substring(line, 40, 42);
      group = substring(line, 42, 44);
      conservation = charAt(line, 44);
      measure = charAt(line, 45);
      catchQuantum = parseDouble(substring(line, 46, 53), Double.NaN);
      catchNumber = parseInt(substring(line, 53, 59), 0);
      target = charAt(line, 59);
      lengthUnit = charAt(line, 60);
      weightOfLengthSample = substring(line, 61, 67);
      lengthSampleNo = parseInt(substring(line, 67, 71), 0);
      individualSampleNo = substring(line, 71, 75);
      otolithShell = charAt(line, 75);
      parasite = charAt(line, 76);
      stomach = charAt(line, 77);
      genetics = charAt(line, 78);
   }

   String getMeasureUnit() {
      return switch (measure) {
         case ' ',   // no measure        (invalid)
              '2'    // volume unit liter (invalid)
               -> "Invalid";

         case '1',   // kg
              '3',   // kg (sløyd uten hode)
              '4'    // kg (sløyd med hode)
               -> "kg";

         case '5',   // tonn
              '6',   // tonn
              '7',   // tonn
              '8'   // tonn
               -> "tonn";

         case '9'    // gram
               -> "gram";

         default -> "kg";
      };
   }

   public double getWeight() {
      return catchQuantum * getWeightFactor();
   }

   private double getWeightFactor() {
      return switch (measure) {
         case ' ',   // no measure        (invalid)
              '2'    // volume unit liter (invalid)
               -> 0;

         case '1',   // kg
              '3',   // kg (sløyd uten hode)
              '4'    // kg (sløyd med hode)
               -> 1;

         case '5',   // tonn
              '6',   // tonn
              '7',   // tonn
              '8'    // tonn
               -> 1000;

         case '9'    // gram
               -> 0.001;

         default -> 1;
      };
   }

   private double getVolume() {
      return switch (measure) {
         case '2'    // volume unit liter
               -> catchQuantum;

         case ' ',   // no measure  (invalid)
              '1',   // kg
              '3',   // kg (sløyd uten hode)
              '4',   // kg (sløyd med hode)
              '5',   // tonn
              '6',   // tonn
              '7',   // tonn
              '8',   // tonn
              '9'    // gram
               -> 0;

         default -> 0;
      };
   }
}
