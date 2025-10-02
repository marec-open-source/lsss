package no.imr.korona.computation.feature;

import no.imr.tools.compile.CompileException;
import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestRunnable;

import java.util.Set;

public final class FeatureRequirementSmoke extends SmokeTestRunnable {
   public FeatureRequirementSmoke() {
   }

   @Override
   public void run() throws CompileException {
      String expression = "count(R200) > 0";
      FeatureRequirement featureRequirement = FeatureRequirementFactory.create(expression);
      assert featureRequirement.isValid(Set.of("R200"));
      Log.global.info(OK + "FeatureRequirement: " + expression);
   }
}
