package no.imr.korona.data.datagrams;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class Bot0DatagramTest {
   @Test
   void addChannel() {
      Bot0Datagram bot0 = new Bot0Datagram(Instant.EPOCH, new double[]{1, 2, 3, 4});
      assertArrayEquals(new double[]{1, 2, 3, 4, 3}, bot0.copyWithAddedChannel(3).getChannelDepths());
   }

   @Test
   void removeChannel() {
      Bot0Datagram bot0 = new Bot0Datagram(Instant.EPOCH, new double[]{1, 2, 3, 4});
      assertArrayEquals(new double[]{2, 3, 4}, bot0.copyWithRemovedChannel(1).getChannelDepths());
      assertArrayEquals(new double[]{1, 3, 4}, bot0.copyWithRemovedChannel(2).getChannelDepths());
      assertArrayEquals(new double[]{1, 2, 4}, bot0.copyWithRemovedChannel(3).getChannelDepths());
      assertArrayEquals(new double[]{1, 2, 3}, bot0.copyWithRemovedChannel(4).getChannelDepths());
   }
}
