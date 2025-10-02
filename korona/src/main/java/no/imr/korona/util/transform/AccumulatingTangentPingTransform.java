package no.imr.korona.util.transform;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.math.linalg.Matrix3;

import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public final class AccumulatingTangentPingTransform extends PingTransform {
   private final Deque<PingIndex> pingIndexes = new ArrayDeque<>();

   public AccumulatingTangentPingTransform(Ping referencePing) {
      super(referencePing);
   }

   public void add(PingIndex pingIndex) {
      pingIndexes.addLast(pingIndex);
      if (pingIndexes.size() > 10) {
         pingIndexes.removeFirst();
      }
   }

   @Override
   protected Matrix3 fallbackHeadingCalculation(Ping ping) {
      Point2D tangent = DataUtils.getTangent(new ArrayList<>(pingIndexes), getMetersPerGeoDegree());
      return PingTransformWithTangent.tangentToRotation(tangent);
   }
}
