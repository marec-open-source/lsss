package no.imr.korona.util;

import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Name;
import no.imr.tools.smoke.SmokeTestRunnable;

public final class ChannelPredicateParameterSmoke extends SmokeTestRunnable {
   public ChannelPredicateParameterSmoke() {
   }

   @Override
   public void run() {
      ChannelPredicateParameter parameter = new ChannelPredicateParameter(new Name("Test"), true);
      parameter.setStringValue("f < 50 || c >= n");
      CompiledChannelPredicate predicate = parameter.getValue().predicate();
      assert predicate.test(38, 1, 10);
      assert !predicate.test(70, 1, 10);
      assert predicate.test(70, 10, 10);

      Log.global.info(OK + "ChannelPredicateParameter: " + parameter.getStringValue());
   }
}
