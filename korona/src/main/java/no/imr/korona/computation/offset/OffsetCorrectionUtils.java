package no.imr.korona.computation.offset;

import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;

import java.util.HashMap;
import java.util.Map;

final class OffsetCorrectionUtils {
   private OffsetCorrectionUtils() {
   }

   static Map<Integer, TransducerParameters> makeChannelToOffsetMap(TransducerParameterManager transducerParameterManager, PingConfiguration pingConfiguration) {
      Map<Integer, TransducerParameters> channelToOffsetMap = new HashMap<>();

      for (TransducerParameters par : transducerParameterManager.getTransducerParametersList()) {
         // Find the (first) channel number for this frequency (if any).
         int channel = getChannel(pingConfiguration.getRawFileConfiguration(), par.getKHz());
         if (channel >= 1) {
            channelToOffsetMap.put(channel, par);
         }
      }
      return channelToOffsetMap;
   }

   private static int getChannel(RawFileConfiguration rawFileConfiguration, int kHz) {
      int channel = 1;
      for (RawFileTransducer transducer : rawFileConfiguration.getTransducers()) {
         if (transducer.getKHz() == kHz) {
            return channel;
         }
         channel++;
      }
      return -1;
   }
}
