package no.imr.korona.computation.dataquality;

import no.imr.korona.data.ping.Ping;

abstract class DataQualityIndicator {
   private final NcVariableInfo variableInfo;

   DataQualityIndicator(NcVariableInfo variableInfo) {
      this.variableInfo = variableInfo;
   }

   NcVariableInfo variableInfo() {
      return variableInfo;
   }

   abstract void processPing(Ping ping);

   abstract float[] computeResult(long[] timeInMillis, float[] bottomDepths);
}
