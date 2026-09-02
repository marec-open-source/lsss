package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.xml.XmlUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class Xml0DatagramTest {
   @Test
   void test() throws IOException, DatagramFormatException {
      String xml = """
            <x a="b">  <b> 1</b> <c>2</c></x>""";
      ByteBuffer byteBuffer = ByteBufferUtils.allocate(100);
      byteBuffer.put(XmlUtils.toDefaultBytes(XmlUtils.readDocument(xml)));
      byteBuffer.put((byte) 0);
      byteBuffer.flip();
      Xml0Datagram parsed = new Xml0Datagram(Instant.EPOCH, byteBuffer);
      assertEquals(xml, XmlUtils.toDefaultString(parsed.getDocument().getRootElement()));
   }
}
