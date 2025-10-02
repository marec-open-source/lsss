package no.imr.korona.computation.dataquality;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ComplexChannelData;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;

import java.util.ArrayList;
import java.util.List;

final class ImpedanceIndicator extends DataQualityIndicator {
   private final int channel;
   private final List<Float> valueList = new ArrayList<>();

   ImpedanceIndicator(int channel) {
      super(new NcVariableInfo("transducer_impedance", Unit.OHM.formalName()));

      this.channel = channel;
   }

   @Override
   void processPing(Ping ping) {
      valueList.add(getImpedance(ping));
   }

   private float getImpedance(Ping ping) {
      if (!(ping.getChannelData(channel) instanceof ComplexChannelData complexChannelData)) {
         return Float.NaN;
      }
      int sectorCount = complexChannelData.getSectorCount();
      double impedanceSum = 0;
      for (int sectorIndex = 0; sectorIndex < sectorCount; sectorIndex++) {
         impedanceSum += complexChannelData.getTransducerImpedanceForSector(sectorIndex).abs();
      }
      return (float) (impedanceSum / sectorCount);
   }

   @Override
   float[] computeResult(long[] timeInMillis, float[] bottomDepths) {
      return Utils.toFloats(valueList);
   }
}
