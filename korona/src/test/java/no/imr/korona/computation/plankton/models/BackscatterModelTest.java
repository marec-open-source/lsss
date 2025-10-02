package no.imr.korona.computation.plankton.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BackscatterModelTest {
   @Test
   void testGaseousSphereModel() {
      GaseousSphereModel gaseousSphereModel = new GaseousSphereModel();
      //used for siphonophores

      float diameter = 0.0006f; //0.6 mm

      double backscatter100 = gaseousSphereModel.getBackscatter(diameter, 100000f);
      double ts100 = 10 * Math.log10(backscatter100);
      assertTrue(ts100 > -74 && ts100 < -69);

      double backscatter200 = gaseousSphereModel.getBackscatter(diameter, 200000f);
      double ts200 = 10 * Math.log10(backscatter200);
      assertTrue(ts200 > -75 && ts200 < -70);

      double backscatter400 = gaseousSphereModel.getBackscatter(diameter, 400000f);
      double ts400 = 10 * Math.log10(backscatter400);
      assertTrue(ts400 > -75 && ts400 < -70);
   }

   @Test
   void testHardShelledSphereModel() {
      HardShelledSphereModel hardShelledSphereModel = new HardShelledSphereModel();
      //used for gastropods

      //set parameters to make this test independent of default parameters
      hardShelledSphereModel.setRFact(0.5f);

      float diameter = 0.0014f; //1.4mm

      double backscatter100 = hardShelledSphereModel.getBackscatter(diameter, 100000f);
      double ts100 = 10 * Math.log10(backscatter100);
      assertTrue(ts100 > -97 && ts100 < -92);

      double backscatter200 = hardShelledSphereModel.getBackscatter(diameter, 200000f);
      double ts200 = 10 * Math.log10(backscatter200);
      assertTrue(ts200 > -85 && ts200 < -75);

      double backscatter400 = hardShelledSphereModel.getBackscatter(diameter, 400000f);
      double ts400 = 10 * Math.log10(backscatter400);
      assertTrue(ts400 > -80 && ts400 < -70);
   }

   @Test
   void testFluidSpheroidModel() {
      FluidProlateSpheroidModel fluidSpheroidModel = new FluidProlateSpheroidModel();
      //used for copepods

      //set parameters to make this test independent of default parameters
      fluidSpheroidModel.setLengthToWidth(5);
      fluidSpheroidModel.setRelativeDensity(1.043f);
      fluidSpheroidModel.setRelativeSoundSpeed(1.052f);

      float diameter = 0.0015f; //1.5mm
      double length = diameter * fluidSpheroidModel.getLengthToWidth();
      double area = Math.pow(length, 2);

      double backscatterKa2 = fluidSpheroidModel.getBackscatter(length, 318300f); //ka = 2
      double tsKa2 = 10 * Math.log10(backscatterKa2 / area);

      assertTrue(tsKa2 > -40 && tsKa2 < -30);
   }

   @Test
   void testFluidBentCylinder() {
      FluidBentCylinderModel fluidBentCylinderModel = new FluidBentCylinderModel();
      //used for euphausiids

      double fluidBentLength = 0.0218;

      //set parameters to make this test independent of default parameters
      fluidBentCylinderModel.setLengthToWidth(8);
      fluidBentCylinderModel.setRFact(0.058f);
      fluidBentCylinderModel.setStdDevLength(0.06f);

      double backscatter100 = fluidBentCylinderModel.getBackscatter(fluidBentLength, 100000f);
      double ts100 = 10 * Math.log10(backscatter100);
      assertTrue(ts100 > -85 && ts100 < -75);

      double backscatter200 = fluidBentCylinderModel.getBackscatter(fluidBentLength, 200000f);
      double ts200 = 10 * Math.log10(backscatter200);
      assertTrue(ts200 > -80 && ts200 < -70);

      double backscatter350 = fluidBentCylinderModel.getBackscatter(fluidBentLength, 350000f);
      double ts350 = 10 * Math.log10(backscatter350);
      assertTrue(ts350 > -90 && ts350 < -80);
   }
}
