package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.range.FloatRange;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

final class LogSvCompressor {
   private final float binStart;
   private final float deltaLogSv;
   private final float addOffset;
   private final int maxBinIndex;

   LogSvCompressor(FloatRange logSvRange, float deltaLogSv) {
      binStart = logSvRange.min() - deltaLogSv; // So that bin index 1 corresponds to [minLogSv, minLogSv + deltaLogSv).
      this.deltaLogSv = deltaLogSv;

      float binMidpoint = (float) KoronaUtils.toDB((KoronaUtils.fromDB(deltaLogSv) + 1) / 2); // The midpoint as sv.
      addOffset = binStart + binMidpoint;
      int wantedMaxBinIndex = (int) Math.ceil((logSvRange.max() - binStart) / deltaLogSv);
      if (wantedMaxBinIndex > 0xffff) {
         wantedMaxBinIndex = 0xffff;
         float actualMaxLogSv = (wantedMaxBinIndex + 1) * deltaLogSv + binStart; // +1 to get the end value of the max bin.
         Log.global.warning("Max log sv reduced to " + actualMaxLogSv + " dB for compressed values to fit in 16 bits");
      }
      maxBinIndex = wantedMaxBinIndex;
   }

   void addAttributes(Variable.Builder<?> builder) {
      builder
            .addAttribute(Attribute.builder().setName(CDM.FILL_VALUE).setDataType(DataType.USHORT).setNumericValue((short) 0, true).build())
            .addAttribute(new Attribute(CDM.ADD_OFFSET, addOffset))
            .addAttribute(new Attribute(CDM.SCALE_FACTOR, deltaLogSv));
   }

   short[] compressSv(float[] sv) {
      short[] compressedValues = new short[sv.length];
      float svError = 0;
      for (int i = 0; i < sv.length; i++) {
         float correctedSv = sv[i] + svError; // This value is always >= 0.
         float logSv = PowerData.svToLogSv(correctedSv);
         int binIndex = (int) Math.floor((logSv - binStart) / deltaLogSv);
         if (binIndex < 1) {
            compressedValues[i] = 0;
            svError = 0;
         } else if (binIndex > maxBinIndex) {
            compressedValues[i] = (short) maxBinIndex;
            svError = 0;
         } else {
            svError = correctedSv - binIndexToSv(binIndex);
            if (i + 1 < sv.length) {
               // Make sure the corrected sv >= 0 for the next bin.
               while (sv[i + 1] + svError < 0) {
                  binIndex--;
                  if (binIndex == 0) {
                     svError = 0;
                     break;
                  }
                  svError = correctedSv - binIndexToSv(binIndex);
               }
            }
            compressedValues[i] = (short) binIndex;
         }
      }
      return compressedValues;
   }

   private float binIndexToSv(int binIndex) {
      float logSv = binIndex * deltaLogSv + addOffset;
      return PowerData.logSvToSv(logSv);
   }
}
