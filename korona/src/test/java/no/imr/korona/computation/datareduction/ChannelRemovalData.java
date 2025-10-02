package no.imr.korona.computation.datareduction;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.configuration.TransmitMode;
import no.imr.korona.test.data.ConstantSyntheticData;

final class ChannelRemovalData extends ConstantSyntheticData {
   ChannelRemovalData() {
      super(1, 0);
   }

   @Override
   protected short getTransmitMode(PingIndex pingIndex, int channel) {
      return channel <= 4 ? TransmitMode.ACTIVE : TransmitMode.PASSIVE;
   }
}
