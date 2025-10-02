package no.imr.korona.computation.plankton.test;

import no.imr.korona.computation.plankton.SizeHistogram;
import no.imr.korona.computation.plankton.models.BackscatterModel;
import no.imr.korona.computation.plankton.models.FluidBentCylinderModel;
import no.imr.korona.computation.plankton.models.FluidProlateSpheroidModel;
import no.imr.korona.computation.plankton.models.GaseousSphereModel;
import no.imr.korona.computation.plankton.models.HardShelledSphereModel;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.math.nnls.ColumnOrderedMatrix;

import java.util.Arrays;
import java.util.Random;

public final class PlanktonInversionSyntheticSyntheticData extends SyntheticData {
   private final HardShelledSphereModel hardShelledSphereModel = new HardShelledSphereModel();
   private final GaseousSphereModel gaseousSphereModel = new GaseousSphereModel();
   private final FluidProlateSpheroidModel fluidProlateSpheroidModel = new FluidProlateSpheroidModel();
   private final FluidBentCylinderModel fluidBentCylinderModel = new FluidBentCylinderModel();

   private double distortionFactor;
   private final Random randomGenerator = new Random();

   private final SizeHistogram hardSizeHistogram = new SizeHistogram(new double[]{5e-4, 10e-4, 10e-4, 15e-4, 15e-4, 20e-4});
   /*
   private final SizeHistogram gasSizeHistogram = new SizeHistogram(new double[]{    5e-4,  10e-4,  10e-4,  15e-4,  15e-4,  20e-4});
   private final SizeHistogram fluidSSizeHistogram = new SizeHistogram(new double[]{    5e-4,  10e-4,  10e-4,  15e-4,  15e-4,  20e-4});
   private final SizeHistogram fbCylSizeHistogram = new SizeHistogram(new double[]{    5e-4,  10e-4,  10e-4,  15e-4,  15e-4,  20e-4});
   */
   private final SizeHistogram gasSizeHistogram = new SizeHistogram(new double[]{5e-4, 9e-4, 9e-4, 14e-4, 14e-4, 17e-4});
   private final SizeHistogram fluidSSizeHistogram = new SizeHistogram(new double[]{100e-4, 115e-4, 115e-4, 140e-4, 140e-4, 160e-4});
   private final SizeHistogram fbCylSizeHistogram = new SizeHistogram(new double[]{15e-4, 20e-4, 20e-4, 25e-4, 25e-4, 30e-4});

   // Bad set
   private final SizeHistogram hardSizeHistogramBad = new SizeHistogram(new double[]{7e-4, 12e-4, 12e-4, 16e-4, 16e-4, 20e-4});
   private final SizeHistogram gasSizeHistogramBad = new SizeHistogram(new double[]{6e-4, 10e-4, 10e-4, 12e-4, 12e-4, 17e-4});
   private final SizeHistogram fluidSSizeHistogramBad = new SizeHistogram(new double[]{130e-4, 150e-4, 150e-4, 180e-4, 180e-4, 200e-4});
   private final SizeHistogram fbCylSizeHistogramBad = new SizeHistogram(new double[]{17e-4, 20e-4, 20e-4, 27e-4, 27e-4, 33e-4});

   public PlanktonInversionSyntheticSyntheticData() {
      hardSizeHistogram.getAbundances()[0] = 2000000000;
      gasSizeHistogram.getAbundances()[0] = 2000000000;
      fluidSSizeHistogram.getAbundances()[0] = 2000000000;
      fbCylSizeHistogram.getAbundances()[0] = 2000000000;
   }

   private static int getCount(PingIndex pingIndex) {
      if (pingIndex.getPingNumber() / 10000 % 2 == 0) {
         return 1000;
      } else {
         return 2000;
      }
   }

   private double calculateSvForModel(String model, double frequency) {
      return switch (model) {
         case "FluidS" -> calculateSv(fluidProlateSpheroidModel, fluidSSizeHistogram, frequency);
         case "Gas" -> calculateSv(gaseousSphereModel, gasSizeHistogram, frequency);
         case "FBCyl", "FBCyl2" -> calculateSv(fluidBentCylinderModel, fbCylSizeHistogram, frequency);
         case "Hard" -> calculateSv(hardShelledSphereModel, hardSizeHistogram, frequency);
         default -> throw new IllegalArgumentException(model);
      };
   }

   private double calculateSvForBadModel(String model, double frequency) {
      return switch (model) {
         case "FluidS" -> calculateSv(fluidProlateSpheroidModel, fluidSSizeHistogramBad, frequency);
         case "Gas" -> calculateSv(gaseousSphereModel, gasSizeHistogramBad, frequency);
         case "FBCyl", "FBCyl2" -> calculateSv(fluidBentCylinderModel, fbCylSizeHistogramBad, frequency);
         case "Hard" -> calculateSv(hardShelledSphereModel, hardSizeHistogramBad, frequency);
         default -> throw new IllegalArgumentException(model);
      };
   }

   private static double calculateSv(BackscatterModel model, SizeHistogram hist, double frequency) {
      ColumnOrderedMatrix matrix = new ColumnOrderedMatrix(1, hist.getAbundances().length);
      for (int i = 0; i < hist.getAbundances().length; i++) {
         matrix.set(0, i, model.getBackscatter(hist.getCenters()[i], frequency));
      }

      double[] sV = new double[1];
      matrix.multiply(hist.getAbundances(), sV);
      return sV[0];
   }

   @Override
   protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
      double sV = 0;
      int dataOffset = 0;

      long remainder = pingIndex.getPingNumber() % 1000;
      long result = pingIndex.getPingNumber() / 1000;

      if (result < 3) {
         fillHistograms(2000000000, 0, 0);

         if (remainder < 220) {
            sV = calculateSvForModel("FluidS", powerData.getFrequency());
         } else if (remainder < 440) {
            sV = calculateSvForModel("Hard", powerData.getFrequency());
            dataOffset = 50;
         } else if (remainder < 660) {
            sV = calculateSvForModel("FBCyl", powerData.getFrequency());
            dataOffset = 100;
         } else if (remainder < 770) {
            sV = calculateSvForModel("FBCyl2", powerData.getFrequency());
            dataOffset = 120;
         } else if (remainder < 880) {
            sV = calculateSvForModel("Gas", powerData.getFrequency());
            dataOffset = 150;
         }
      } else if (result == 3) {
         fillHistograms(2000000000, 0, 1000000000);

         if (remainder < 220) {
            sV = calculateSvForModel("FluidS", powerData.getFrequency());
         } else if (remainder < 440) {
            sV = calculateSvForModel("Hard", powerData.getFrequency());
            dataOffset = 50;
         } else if (remainder < 660) {
            sV = calculateSvForModel("FBCyl", powerData.getFrequency());
            dataOffset = 100;
         } else if (remainder < 770) {
            sV = calculateSvForModel("FBCyl2", powerData.getFrequency());
            dataOffset = 120;
         } else if (remainder < 880) {
            sV = calculateSvForModel("Gas", powerData.getFrequency());
            dataOffset = 150;
         }
      } else if (result == 4) {
         fillBadHistograms(2000000000, 0, 0);

         if (remainder < 220) {
            sV = calculateSvForBadModel("FluidS", powerData.getFrequency());
         } else if (remainder < 440) {
            sV = calculateSvForBadModel("Hard", powerData.getFrequency());
            dataOffset = 50;
         } else if (remainder < 660) {
            sV = calculateSvForBadModel("FBCyl", powerData.getFrequency());
            dataOffset = 100;
         } else if (remainder < 770) {
            sV = calculateSvForBadModel("FBCyl2", powerData.getFrequency());
            dataOffset = 120;
         } else if (remainder < 880) {
            sV = calculateSvForBadModel("Gas", powerData.getFrequency());
            dataOffset = 150;
         }
      }

      /*
      else if (result == 4) {
         fillHistograms(2000000000, 0, 0);

         if (remainder < 220) {
            sV = calculateSvForModel("FluidS", powerData.getFrequency()) + calculateSvForModel("Hard", powerData.getFrequency());
         } else if (remainder < 440) {
            sV = calculateSvForModel("FluidS", powerData.getFrequency()) + calculateSvForModel("Gas", powerData.getFrequency());
            dataOffset = 50;
         } else if (remainder < 660) {
            sV = calculateSvForModel("FluidS", powerData.getFrequency()) + calculateSvForModel("FBCyl", powerData.getFrequency());
            dataOffset = 100;
         } else if (remainder < 880) {
            sV = calculateSvForModel("Gas", powerData.getFrequency()) + calculateSvForModel("Hard", powerData.getFrequency());
            dataOffset = 150;
         }
      }
      */

      float[] sVf = new float[getCount(pingIndex)];

      if (result == 0) {
         distortionFactor = 0.0;
      }
      if (result == 1) {
         distortionFactor = 0.5;
      }
      if (result == 2) {
         distortionFactor = 1.0;
      }
      if (result == 3) {
         distortionFactor = 0.0;
      }
      if (result == 4) {
         distortionFactor = 0.0;
      }

      for (int i = 0; i < dataOffset; i++) {
         sVf[i] = 0;
      }

      for (int i = dataOffset; i < sVf.length; i++) {
         sVf[i] = (float) (sV * (1 + randomGenerator.nextDouble(-1, 1) * distortionFactor));
      }

      int index = powerData.depthToSampleIndex(getBottomDepth(pingIndex, powerData.getChannel()));
      Arrays.fill(sVf, index, index + 10, 1e5f);

      powerData.setSv(sVf);
   }

   private void fillHistograms(double a, double b, double c) {
      hardSizeHistogram.getAbundances()[0] = a;
      gasSizeHistogram.getAbundances()[0] = a;
      fluidSSizeHistogram.getAbundances()[0] = a;
      fbCylSizeHistogram.getAbundances()[0] = a;

      hardSizeHistogram.getAbundances()[1] = b;
      gasSizeHistogram.getAbundances()[1] = b;
      fluidSSizeHistogram.getAbundances()[1] = b;
      fbCylSizeHistogram.getAbundances()[1] = b;

      hardSizeHistogram.getAbundances()[1] = c;
      gasSizeHistogram.getAbundances()[1] = c;
      fluidSSizeHistogram.getAbundances()[1] = c;
      fbCylSizeHistogram.getAbundances()[1] = c;
   }

   private void fillBadHistograms(double a, double b, double c) {
      hardSizeHistogramBad.getAbundances()[0] = a;
      gasSizeHistogramBad.getAbundances()[0] = a;
      fluidSSizeHistogramBad.getAbundances()[0] = a;
      fbCylSizeHistogramBad.getAbundances()[0] = a;

      hardSizeHistogramBad.getAbundances()[1] = b;
      gasSizeHistogramBad.getAbundances()[1] = b;
      fluidSSizeHistogramBad.getAbundances()[1] = b;
      fbCylSizeHistogramBad.getAbundances()[1] = b;

      hardSizeHistogramBad.getAbundances()[1] = c;
      gasSizeHistogramBad.getAbundances()[1] = c;
      fluidSSizeHistogramBad.getAbundances()[1] = c;
      fbCylSizeHistogramBad.getAbundances()[1] = c;
   }

   @Override
   protected float getBottomDepth(PingIndex pingIndex, int channel) {
      return 150;
   }
}
