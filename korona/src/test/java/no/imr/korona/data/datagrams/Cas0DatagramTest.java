package no.imr.korona.data.datagrams;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class Cas0DatagramTest {
   private static final byte CAT1 = 1;
   private static final byte CAT2 = 3;

   private static final float DISC1 = 0.3f;
   private static final float DISC2 = 0.4f;
   private static final float PROB1 = 0.6f;
   private static final float PROB2 = 0.2f;

   @Test
   void cas0ByteBufferTest() {
      Cas0Datagram cas0Datagram = new Cas0Datagram(Instant.ofEpochSecond(111111111), 3, 4);

      cas0Datagram.setCategory(0, CAT1, DISC1, PROB1);
      cas0Datagram.setCategory(1, CAT2, DISC2, PROB2);

      ByteBuffer byteBuffer = ByteBuffer.allocate(1000);

      cas0Datagram.write(byteBuffer);

      byteBuffer.rewind();

      Cas0Datagram cas0DatagramRead = new Cas0Datagram(Instant.ofEpochSecond(111111111), byteBuffer);

      assertEquals(CAT1, cas0DatagramRead.getBestCategory());
      assertEquals(CAT2, cas0DatagramRead.getCategory(1));

      assertEquals(DISC1, cas0DatagramRead.getBestDiscriminant(), 0.01f);
      assertEquals(DISC2, cas0DatagramRead.getDiscriminant(1), 0.01f);

      assertEquals(PROB1, cas0DatagramRead.getBestProbability(), 0.01f);
      assertEquals(PROB2, cas0DatagramRead.getProbability(1), 0.01f);

      assertEquals(4, cas0DatagramRead.getRegionId());
   }
}
