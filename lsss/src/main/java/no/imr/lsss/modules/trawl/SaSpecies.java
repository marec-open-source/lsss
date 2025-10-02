package no.imr.lsss.modules.trawl;

abstract sealed class SaSpecies {
   private static final double wsl = 25.0; // Sveipebredde (avhengig av trål-type, art, lengde og fangstdyp) (et mål på redskapens fangstevne)

   private SaSpecies() {
   }

   static final class SaFish extends SaSpecies {
      private final int group;        // Species group number
      private final String name;      // Acoustic category name
      private final double x;         // Parameter in TS formula: TS = x*lg(L) - y; (L = length(cm))
      private final double y;         // Parameter in TS formula: TS = x*lg(L) - y; (L = length(cm))
      private final double a;         // Parameter in sweepwidth correction formula: K = a*L(**b); (L = length(cm))
      private final double b;         // Parameter in sweepwidth correction formula: K = a*L(**b); (L = length(cm))
      private final int limitLower;   // Lower length limit for correction
      private final int limitUpper;   // Upper length limit for correction

      SaFish(String line) {
         String[] parts = line.split(";");
         if (!parts[0].equals("F")) {
            throw new IllegalArgumentException(line);
         }
         group = Integer.parseInt(parts[1].trim());
         name = parts[2].trim();
         x = Double.parseDouble(parts[3].trim());
         y = Double.parseDouble(parts[4].trim());
         a = Double.parseDouble(parts[5].trim());
         b = Double.parseDouble(parts[6].trim());
         limitLower = Integer.parseInt(parts[7].trim());
         limitUpper = Integer.parseInt(parts[8].trim());
      }

      double getTS(double L) {
         return x * Math.log10(L) - y;
      }

      double getSigma(double L) {
         return 4.0 * Math.PI * Math.pow(10.0, getTS(L) / 10.0);
      }

      double getSweepWidth() {
         return wsl;
      }

      double getSweepWithCorrected(double L) {
         return wsl * a * Math.pow(L, b);
      }
   }

   static final class SaPlankton extends SaSpecies {
      private final int group;        // Species group number
      private final String name;      // Acoustic category name
      private final double ts;        // TS
      private final double k;         // constant for weight --> number conversion

      SaPlankton(String line) {
         String[] parts = line.split(";");
         if (!parts[0].equals("P")) {
            throw new IllegalArgumentException(line);
         }
         group = Integer.parseInt(parts[1].trim());
         name = parts[2].trim();
         ts = Double.parseDouble(parts[3].trim());
         k = Double.parseDouble(parts[4].trim());
      }

      double getWeightNumberConstant() {
         return k;
      }

      double getTS() {
         return ts;
      }

      double getSigma() {
         return 4.0 * Math.PI * Math.pow(10.0, getTS() / 10.0);
      }

      double getSweepWidth() {
         return wsl;
      }
   }
}
