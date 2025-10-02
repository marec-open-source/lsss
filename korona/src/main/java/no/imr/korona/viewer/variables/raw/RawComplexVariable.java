package no.imr.korona.viewer.variables.raw;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.ComplexChannelData;
import no.imr.korona.util.KoronaUtils;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.range.FloatRange;

public final class RawComplexVariable extends ContinuousRawVariable {
   RawComplexVariable() {
      super(new Name("complex", "Complex"),
            new ContinuousVariableSettings(FloatRange.of(-100, 20), FloatRange.of(-90, 0), 1, false),
            Unit.NONE, ExportTransform.identity());
   }

   @Override
   public ContinuousVariableResult evaluate(int channel, Ping ping) {
      ChannelData channelData = ping.getChannelData(channel);
      if (!(channelData instanceof ComplexChannelData complexChannelData)) {
         return ContinuousVariableResult.EMPTY;
      }
      float[][] real = complexChannelData.getReal();
      float[][] imag = complexChannelData.getImag();
      int count = complexChannelData.getCount();
      float[] floatData = new float[count];
      int sectorCount = complexChannelData.getSectorCount();
      for (int i = 0; i < count; i++) {
         double re = 0;
         double im = 0;
         for (int sector = 0; sector < sectorCount; sector++) {
            re += real[sector][i];
            im += imag[sector][i];
         }
         re /= sectorCount;
         im /= sectorCount;
         floatData[i] = (float) KoronaUtils.toDB(Math.sqrt(re * re + im * im));
      }
      return new ContinuousVariableResult(floatData, complexChannelData.getDepthRange());
   }
}
