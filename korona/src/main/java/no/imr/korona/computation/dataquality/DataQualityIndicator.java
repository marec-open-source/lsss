package no.imr.korona.computation.dataquality;

import no.imr.korona.data.ping.Ping;

import java.time.Instant;

abstract class DataQualityIndicator {
   private final NcVariableInfo variableInfo;

   DataQualityIndicator(NcVariableInfo variableInfo) {
      this.variableInfo = variableInfo;
   }

   NcVariableInfo variableInfo() {
      return variableInfo;
   }

   abstract void processPing(Ping ping);

   abstract float[] computeResult(Instant[] instants, float[] bottomDepths);
}
