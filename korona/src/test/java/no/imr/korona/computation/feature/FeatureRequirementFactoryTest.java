package no.imr.korona.computation.feature;

import no.imr.tools.compile.CompileException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class FeatureRequirementFactoryTest {
   @Test
   void blank() throws CompileException {
      FeatureRequirement featureRequirement = FeatureRequirementFactory.create(" ");
      assertTrue(featureRequirement.isValid(Set.of("R38")));
      assertTrue(featureRequirement.isValid(Set.of("R70", "R200")));
   }

   @Test
   void simple() throws CompileException {
      FeatureRequirement featureRequirement = FeatureRequirementFactory.create("R200");
      assertTrue(featureRequirement.isValid(Set.of("R200")));
      assertTrue(featureRequirement.isValid(Set.of("R70", "R200")));
      assertFalse(featureRequirement.isValid(Set.of("R70")));
   }

   @Test
   void advanced() throws CompileException {
      FeatureRequirement featureRequirement = FeatureRequirementFactory.create("R200 && R200 && count(R18, R70) >= 1");
      assertFalse(featureRequirement.isValid(Set.of("R200")));
      assertTrue(featureRequirement.isValid(Set.of("R18", "R200")));
      assertTrue(featureRequirement.isValid(Set.of("R70", "R200")));
      assertFalse(featureRequirement.isValid(Set.of("R18", "R70")));
   }

   @Test
   void additionalFeatures() throws CompileException {
      FeatureRequirement featureRequirement = FeatureRequirementFactory.create("depth || Sv38");
      assertFalse(featureRequirement.isValid(Set.of("R200")));
      assertTrue(featureRequirement.isValid(Set.of("depth")));
      assertTrue(featureRequirement.isValid(Set.of("Sv38")));
   }
}
