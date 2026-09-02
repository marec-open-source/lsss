package no.imr.korona.viewer.variables.plankton;

import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.datagrams.Pid0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Residual.
 */
public final class ResidualVariable extends ContinuousPlanktonVariable {
   ResidualVariable() {
      super(new Name("residual", "Residual"), new ContinuousVariableSettings(FloatRange.of(0, 0.2f), FloatRange.of(0.05f, 0.1f), 0.001, true),
            Unit.NONE, ExportTransform.round(10_000));
   }

   @Override
   public @Nullable ContinuousVariableResult evaluate(int channel, Ping ping) {
      Pic0Datagram pic0Datagram = getPic0Datagram();
      Pid0Datagram pid0Datagram = ping.getPingItem(Pid0Datagram.class);
      if (pic0Datagram == null || pid0Datagram == null) {
         return null;
      }

      List<Pid0Datagram.PlanktonSample> planktonSamples = pid0Datagram.getPlanktonSamples(pic0Datagram);
      float[] floatData = new float[planktonSamples.size()];
      for (int i = 0; i < floatData.length; i++) {
         floatData[i] = planktonSamples.get(i).getBestPlanktonData().getResidual();
      }
      return ContinuousVariableResult.of(floatData, pid0Datagram.getDepthRange());
   }
}
