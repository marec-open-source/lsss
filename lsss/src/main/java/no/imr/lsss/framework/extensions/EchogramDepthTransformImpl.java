package no.imr.lsss.framework.extensions;

import no.imr.korona.data.ping.PingMapping;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationZSettings;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.echogram.EchogramDepthTransform;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

final class EchogramDepthTransformImpl implements EchogramDepthTransform {
   private final LSSS lsss;
   private final InterpretationZSettings zSettings;

   EchogramDepthTransformImpl(LSSS lsss, InterpretationZSettings zSettings) {
      this.lsss = lsss;
      this.zSettings = zSettings;
   }

   @Override
   public double depthToY(double depth, Instant instant) {
      no.imr.korona.data.ping.PingIndex lsssPingIndex = toPingIndex(instant);
      return lsssPingIndex != null ? zSettings.depthToY((float) depth, lsssPingIndex) : Double.NaN;
   }

   @Override
   public double depthToY(double depth, PingIndex pingIndex) {
      no.imr.korona.data.ping.PingIndex lsssPingIndex = toLsssPingIndex(pingIndex);
      return zSettings.depthToY((float) depth, lsssPingIndex);
   }

   @Override
   public double yToDepth(double y, Instant instant) {
      no.imr.korona.data.ping.PingIndex lsssPingIndex = toPingIndex(instant);
      return lsssPingIndex != null ? zSettings.yToDepth(y, lsssPingIndex) : Double.NaN;
   }

   @Override
   public double yToDepth(double y, PingIndex pingIndex) {
      no.imr.korona.data.ping.PingIndex lsssPingIndex = toLsssPingIndex(pingIndex);
      return zSettings.yToDepth(y, lsssPingIndex);
   }

   private no.imr.korona.data.ping.@Nullable PingIndex toPingIndex(Instant instant) {
      return lsss.getDataManager().getDataFileSet().getContainingPingIndex(
            PingMapping.instantToTimeValue(instant), PingMapping.TIME);
   }

   private no.imr.korona.data.ping.PingIndex toLsssPingIndex(PingIndex pingIndex) {
      return pingIndex instanceof no.imr.korona.data.ping.PingIndex lsssPingIndex
            ? lsssPingIndex
            : lsss.getDataManager().getDataFileSet().getPingIndex(pingIndex.getPingNumber());
   }
}
