package no.imr.lsss.modules.broadband;

import no.imr.korona.computation.feature.FrequencyMapping;
import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.TransformedNumberAxis;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeBuilder;

import java.util.List;

public final class BroadbandModuleUtils {
   private BroadbandModuleUtils() {
   }

   public static TransformedNumberAxis kHzAxis(List<Graph> graphs, FrequencyMapping mapping) {
      FloatRange kHzRange = kHzRange(graphs);
      return mapping.axis(kHzRange);
   }

   private static FloatRange kHzRange(List<Graph> graphs) {
      return graphs.stream()
            .flatMap(graph -> graph.getPoints().stream())
            .mapToDouble(Vec2::x)
            .collect(FloatRangeBuilder::new, FloatRangeBuilder::expand, FloatRangeBuilder::expand)
            .toFloatRange();
   }
}
