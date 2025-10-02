package no.imr.korona.computation.towfish;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.tools.Utils;

/**
 * Merges raw datagrams.
 */
final class RawMerger {
   private RawMerger() {
   }

   static void merge(Ping basePing, Ping mergePing, float mergePingStartMeterOffset) {
      mergePing.getNonNullPowerDatas().forEach(mergeRaw -> {
         int baseChannel = basePing.getRawFileConfiguration().lastChannelWithKHz(Utils.hzToKHz(mergeRaw.getFrequency()));
         if (baseChannel <= 0) {
            return;
         }
         PowerData baseRaw = basePing.getPowerData(baseChannel);
         if (baseRaw == null) {
            return;
         }
         int mergeStartOffset = mergeRaw.rangeToContainingSampleIndex(mergePingStartMeterOffset);
         merge(baseRaw, mergeRaw, mergeStartOffset);
      });
   }

   private static void merge(PowerData basePowerData, PowerData mergePowerData, int mergePingStartOffset) {
      ResampledFloatArray resampledMergeSv = ResampledFloatArray.create(mergePowerData.getSv(), mergePowerData, basePowerData, mergePingStartOffset);

      float[] baseSv = basePowerData.getSv();
      int baseBegin = resampledMergeSv.getBeginReferenceIndex();
      int baseEnd = resampledMergeSv.getEndReferenceIndex();

      if (baseEnd < 0) {
         return;
      }

      if (baseBegin < 0) {
         // Ignore merge data above base data
         baseBegin = 0;
      }

      if (baseEnd > baseSv.length) {
         float[] oldBaseSv = baseSv;
         basePowerData.setCount(baseEnd);
         baseSv = basePowerData.getSv();
         System.arraycopy(oldBaseSv, 0, baseSv, 0, Math.min(baseBegin, oldBaseSv.length));
      }

      float[] mergeSv = resampledMergeSv.values();
      int mergeBegin = resampledMergeSv.referenceIndexToResampledIndex(baseBegin);

      System.arraycopy(mergeSv, mergeBegin, baseSv, baseBegin, baseEnd - baseBegin);

      basePowerData.setSv(baseSv);
   }
}
