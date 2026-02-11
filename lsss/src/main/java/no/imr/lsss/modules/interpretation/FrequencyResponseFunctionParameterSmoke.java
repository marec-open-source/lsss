package no.imr.lsss.modules.interpretation;

import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Name;
import no.imr.tools.smoke.SmokeTestRunnable;

public final class FrequencyResponseFunctionParameterSmoke extends SmokeTestRunnable {
   public FrequencyResponseFunctionParameterSmoke() {
   }

   @Override
   public void run() {
      FrequencyResponseFunctionParameter p = new FrequencyResponseFunctionParameter(new Name("Test"));
      p.setStringValue("F + 123");
      checkEquals(10_123.0, p.getFunction().eval(10_000));
      Log.global.info(OK + "FrequencyResponseFunctionParameter: " + p.getStringValue());
   }
}
