package no.imr.korona.data.datagrams;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DatagramTypeManagerTest {
   @Test
   void testGetDatagramType() {
      DatagramTypeManager datagramTypeManager = new DatagramTypeManager();

      for (DatagramType datagramType : datagramTypeManager.getDatagramTypes()) {
         assertSame(datagramType, datagramTypeManager.getDatagramType(datagramType.getIntCode()));

         assertEquals(datagramType.getAsciiQuad(), DatagramType.toAsciiQuad(datagramType.getIntCode()));
         assertEquals(datagramType.getIntCode(), DatagramType.toIntCode(datagramType.getAsciiQuad()));
      }
   }
}
