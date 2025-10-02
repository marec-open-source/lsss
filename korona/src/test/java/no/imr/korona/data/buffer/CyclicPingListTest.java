package no.imr.korona.data.buffer;

import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CyclicPingListTest {
   @Test
   void testSetEndPing() {
      PingBuffer pingBuffer = new BoundedPingBuffer(100);
      List<PingIndex> pingIndices = new ArrayList<>();
      SyntheticData syntheticData = new ConstantSyntheticData();
      for (int i = 0; i < 30; i++) {
         PingIndex pingIndex = syntheticData.createPingIndex(i);
         pingIndices.add(pingIndex);
         Ping ping = syntheticData.createPing(pingIndex);
         pingBuffer.newPing(ping);
      }

      DummyCyclicPingList list = new DummyCyclicPingList(pingBuffer, 10);

      test(list, pingIndices.get(2), 0, 0, 10, new int[]{-1, -1, -1, -1, -1, -1, -1, 0, 1, 2});
      test(list, pingIndices.get(7), 5, 0, 5, new int[]{3, 4, 5, 6, 7, -1, -1, 0, 1, 2});
      test(list, pingIndices.get(4), 2, 2, 3, new int[]{3, 4, -1, -1, -1, -1, -1, 0, 1, 2});
      test(list, pingIndices.get(11), 9, 2, 7, new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 2});
      test(list, pingIndices.get(15), 3, 9, 4, new int[]{13, 14, 15, 6, 7, 8, 9, 10, 11, 12});
      test(list, pingIndices.get(11), 9, 9, 4, new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 2});
      test(list, pingIndices.get(12), 0, 9, 1, new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
      test(list, pingIndices.get(13), 1, 0, 1, new int[]{13, 4, 5, 6, 7, 8, 9, 10, 11, 12});
      test(list, pingIndices.get(14), 2, 1, 1, new int[]{13, 14, 5, 6, 7, 8, 9, 10, 11, 12});
   }

   private static void test(DummyCyclicPingList list, PingIndex pingIndex, int next, int begin, int count, int[] pingNumbers) {
      list.setEndPing(pingIndex);
      assertEquals(begin, list.begin);
      assertEquals(count, list.count);
      assertEquals(next, list.getNext());
      Ping endPing = list.getPings().get(list.index(list.getNext() - 1));
      assertNotNull(endPing);
      assertEquals(pingIndex, endPing.getPingIndex());

      for (int i = 0; i < list.getSize(); i++) {
         Ping ping = list.getPings().get(i);
         int pingNumber = (int) (ping != null ? ping.getPingIndex().getPingNumber() : -1);
         assertEquals(pingNumbers[i], pingNumber);
      }
   }

   private static final class DummyCyclicPingList extends CyclicPingList {
      private int begin;
      private int count;

      private DummyCyclicPingList(PingBuffer pingBuffer, int size) {
         super(pingBuffer, size);
      }

      @Override
      protected void updated(int begin, int count) {
         this.begin = begin;
         this.count = count;
      }
   }
}
