package no.imr.korona.viewer.variables.raw;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

public final class TsuVariable extends ContinuousRawVariable {
   TsuVariable() {
      super(new Name("tsu", "TSU"), new ContinuousVariableSettings(FloatRange.of(-99, -18), FloatRange.of(-82, -30), 1, false),
            Unit.DB, ExportRounding.db());
   }

   @Override
   public ContinuousVariableResult evaluate(int channel, Ping ping) {
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null) {
         return ContinuousVariableResult.EMPTY;
      }

      float[] floatData = new float[powerData.getCount()];
      for (int i = 0; i < floatData.length; i++) {
         floatData[i] = powerData.getTSU(i);
      }

      return new ContinuousVariableResult(floatData, powerData.getDepthRange());
   }
}
