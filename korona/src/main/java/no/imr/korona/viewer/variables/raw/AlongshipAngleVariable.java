package no.imr.korona.viewer.variables.raw;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

/**
 * Alongship angle.
 */
public final class AlongshipAngleVariable extends ContinuousRawVariable {
   AlongshipAngleVariable() {
      super(new Name("alongshipAngle", "Alongship angle"), new ContinuousVariableSettings(FloatRange.of(-15, 15), FloatRange.of(-10, 10), 0.1, false),
            Unit.DEGREES, ExportRounding.degrees());
   }

   @Override
   public ContinuousVariableResult evaluate(int channel, Ping ping) {
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null || powerData.getAngleData() == null) {
         return ContinuousVariableResult.EMPTY;
      }

      float[] floatData = new float[powerData.getCount()];
      for (int i = 0; i < floatData.length; i++) {
         floatData[i] = powerData.getMechanicalAlongAngle(i);
      }

      return new ContinuousVariableResult(floatData, powerData.getDepthRange());
   }
}
