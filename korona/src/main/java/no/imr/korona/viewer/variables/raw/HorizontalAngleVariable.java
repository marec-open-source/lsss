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

/**
 * Horizontal angle.
 */
public final class HorizontalAngleVariable extends ContinuousRawVariable {
   HorizontalAngleVariable() {
      super(new Name("horizontalAngle", "Horizontal angle"), new ContinuousVariableSettings(FloatRange.of(-180, 180), FloatRange.of(-120, 120), 0.1, false),
            Unit.DEGREES, ExportRounding.degrees());
   }

   @Override
   public @Nullable ContinuousVariableResult evaluate(int channel, Ping ping) {
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null || powerData.getAngleData() == null) {
         return null;
      }

      float[] floatData = new float[powerData.getCount()];
      for (int i = 0; i < floatData.length; i++) {
         float along = powerData.getMechanicalAlongAngle(i);
         float athwart = powerData.getMechanicalAthwartAngle(i);
         floatData[i] = (float) Math.toDegrees(Math.atan2(along, athwart));
      }

      return ContinuousVariableResult.of(floatData, powerData.getDepthRange());
   }
}
