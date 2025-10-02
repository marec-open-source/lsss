package no.imr.korona.data.datagrams;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class Cad0DatagramTest {
   @Test
   void floatToByte() {
      assertEquals((byte) 0, Cad0Datagram.floatToByte(-1));
      assertEquals((byte) 0, Cad0Datagram.floatToByte(0));
      assertEquals((byte) 127, Cad0Datagram.floatToByte(0.5f));
      assertEquals((byte) 255, Cad0Datagram.floatToByte(1));
      assertEquals((byte) 255, Cad0Datagram.floatToByte(2));
   }

   @Test
   void byteToFloat() {
      assertEquals(0, Cad0Datagram.byteToFloat((byte) 0));
      assertEquals(0.5f, Cad0Datagram.byteToFloat((byte) 127), 1f / 255);
      assertEquals(1, Cad0Datagram.byteToFloat((byte) 255));
   }
}
