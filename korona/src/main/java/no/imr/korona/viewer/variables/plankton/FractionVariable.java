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

import java.util.List;

/**
 * Fraction.
 */
public final class FractionVariable extends ContinuousPlanktonVariable {
   FractionVariable() {
      super(new Name("fraction", "Fraction"), new ContinuousVariableSettings(FloatRange.of(0, 1), FloatRange.of(0.1f, 0.9f), 0.01, true),
            Unit.NONE, ExportTransform.round(1000));
   }

   @Override
   public ContinuousVariableResult evaluate(int channel, Ping ping) {
      Pic0Datagram pic0Datagram = getPic0Datagram();
      Pid0Datagram pid0Datagram = ping.getPingItem(Pid0Datagram.class);
      if (pic0Datagram == null || pid0Datagram == null) {
         return ContinuousVariableResult.EMPTY;
      }

      List<Pid0Datagram.PlanktonSample> planktonSamples = pid0Datagram.getPlanktonSamples(pic0Datagram);
      float[] floatData = new float[planktonSamples.size()];
      for (int i = 0; i < floatData.length; i++) {
         floatData[i] = planktonSamples.get(i).getBestPlanktonData().getFraction();
      }
      return new ContinuousVariableResult(floatData, pid0Datagram.getDepthRange());
   }
}
