package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.DatagramType;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.LsssDatagram;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class GraphicalInfoSubDatagramTest {
   @Test
   void testSerialization() throws DatagramFormatException {
      GraphicalInfoSubDatagram graphicalInfoSubDatagram = new GraphicalInfoSubDatagram(0);
      List<EchogramOffsetPoint> points = List.of(
            new EchogramOffsetPoint(10, 20),
            new EchogramOffsetPoint(0, 20),
            new EchogramOffsetPoint(0, 50),
            new EchogramOffsetPoint(10, 50)
      );
      graphicalInfoSubDatagram.addGraphicalObject(new OffsetPolygon(List.of("abcd"), Color.RED, true, points));
      graphicalInfoSubDatagram.addGraphicalObject(new OffsetPolygon(List.of("a", "b"), Color.BLUE, false, points));
      graphicalInfoSubDatagram.addText("a");
      graphicalInfoSubDatagram.addText("b");
      graphicalInfoSubDatagram.addText("c");

      LsssDatagram lsssDatagram = new LsssDatagram(graphicalInfoSubDatagram);
      ByteBuffer byteBuffer = lsssDatagram.toByteBufferIncludingHeader();

      DatagramTypeManager datagramTypeManager = new DatagramTypeManager();
      int intCode = byteBuffer.getInt();
      DatagramType datagramType = datagramTypeManager.getDatagramType(intCode);
      assertEquals(LsssDatagram.TYPE, datagramType);
      long ntDate = byteBuffer.getLong();
      LsssDatagram lsssDatagramFromByteBuffer = new LsssDatagram(ntDate, byteBuffer, datagramTypeManager);
      assertEquals(0, byteBuffer.remaining());

      testEquals(graphicalInfoSubDatagram, (GraphicalInfoSubDatagram) lsssDatagramFromByteBuffer.getSubDatagram());

      testEquals(graphicalInfoSubDatagram, (GraphicalInfoSubDatagram) lsssDatagram.makeCopy().getSubDatagram());
   }

   private static void testEquals(GraphicalInfoSubDatagram datagram1, GraphicalInfoSubDatagram datagram2) {
      assertEquals(datagram1.getTexts(), datagram2.getTexts());
      assertEquals(datagram1.getGraphicalObjects(), datagram2.getGraphicalObjects());
   }
}
