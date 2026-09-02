package no.imr.korona.viewer.variables.raw;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

public final class RelativeFrequencyResponseVariable extends ContinuousRawVariable {
   private int referenceKHz = 38;

   RelativeFrequencyResponseVariable() {
      super(new Name("relativeFrequencyResponse", "R(f)"),
            new ContinuousVariableSettings(FloatRange.of(-50, 50), FloatRange.of(-25, 25), 1, false),
            Unit.DB, ExportRounding.db());
   }

   public void setReferenceKHz(int kHz) {
      if (referenceKHz != kHz) {
         referenceKHz = kHz;
         getSettings().getChangeManager().notifyListeners();
      }
   }

   @Override
   public @Nullable ContinuousVariableResult evaluate(int channel, Ping ping) {
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null) {
         return null;
      }

      int referenceChannel = ping.getRawFileConfiguration().lastChannelWithKHz(referenceKHz);
      if (referenceChannel <= 0) {
         return null;
      }
      PowerData referencePowerData = ping.getPowerData(referenceChannel);
      if (referencePowerData == null) {
         return null;
      }

      ResampledFloatArray resampledSv = ResampledFloatArray.create(powerData.getLogSv(), powerData, referencePowerData);
      int beginIndex = Math.max(resampledSv.getBeginReferenceIndex(), 0);
      int endIndex = Math.min(resampledSv.getEndReferenceIndex(), referencePowerData.getCount());
      if (beginIndex >= endIndex) {
         return null;
      }

      float[] referenceLogSv = referencePowerData.getLogSv();
      float[] floatData = new float[endIndex - beginIndex];
      for (int i = beginIndex; i < endIndex; i++) {
         floatData[i - beginIndex] = resampledSv.getValueForReferenceIndex(i) - referenceLogSv[i];
      }

      float minDepth = referencePowerData.getSampleDepth(beginIndex);
      float maxDepth = referencePowerData.getSampleDepth(endIndex);
      return ContinuousVariableResult.of(floatData, FloatRange.of(minDepth, maxDepth));
   }
}
