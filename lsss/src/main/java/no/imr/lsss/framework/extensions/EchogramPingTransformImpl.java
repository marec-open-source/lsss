package no.imr.lsss.framework.extensions;

import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.Utils;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.echogram.EchogramPingTransform;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

final class EchogramPingTransformImpl implements EchogramPingTransform {
   private final InterpretationSettings interpretationSettings;
   private final EchogramPingSettings pingSettings;

   EchogramPingTransformImpl(InterpretationSettings interpretationSettings) {
      this.interpretationSettings = interpretationSettings;
      pingSettings = interpretationSettings.getPingSettings();
   }

   @Override
   public double instantToX(Instant time) {
      return pingSettings.ntDateToX(NTDate.instantToNTDate(time));
   }

   @Override
   public Instant xToInstant(double x) {
      return NTDate.ntDateToInstant(pingSettings.xToNTDate(x));
   }

   @Override
   public @Nullable PingIndex xToContainingSubsampledPingIndex(double x) {
      List<no.imr.korona.data.ping.PingIndex> pingIndices = interpretationSettings.getPingSampler().getRequestedPingIndices();
      int i = Utils.binarySearchForDouble(pingIndices, x, pingSettings::pingIndexToX);
      if (i >= 0) {
         return pingIndices.get(i);
      }
      int insertionPoint = -(i + 1);
      if (insertionPoint > 0 && insertionPoint < pingIndices.size()) {
         return pingIndices.get(insertionPoint);
      }
      if (pingIndices.isEmpty()) {
         return null;
      }
      if (insertionPoint == 0 && x >= 0) {
         return pingIndices.getFirst();
      }
      if (insertionPoint == pingIndices.size() && x <= pingSettings.getWidth()) {
         return pingIndices.getLast();
      }
      return null;
   }
}
