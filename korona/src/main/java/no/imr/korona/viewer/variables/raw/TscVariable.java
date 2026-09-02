package no.imr.korona.viewer.variables.raw;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

public final class TscVariable extends ContinuousRawVariable {
   TscVariable() {
      super(new Name("tsc", "TSC"), new ContinuousVariableSettings(FloatRange.of(-99, -18), FloatRange.of(-82, -30), 1, false),
            Unit.DB, ExportRounding.db());
   }

   @Override
   public @Nullable ContinuousVariableResult evaluate(int channel, Ping ping) {
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null || powerData.getAngleData() == null) {
         return null;
      }

      float[] floatData = new float[powerData.getCount()];
      for (int i = 0; i < floatData.length; i++) {
         floatData[i] = powerData.getTSC(i);
      }

      return ContinuousVariableResult.of(floatData, powerData.getDepthRange());
   }
}
