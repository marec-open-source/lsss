package no.imr.korona.data.datagrams;

import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * General purpose XML datagram.
 */
public final class Xml0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("XML0", Xml0Datagram::new);

   private final Document document;

   public Xml0Datagram(long ntDate, Document document) {
      super(ntDate);

      this.document = document;
   }

   public Xml0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      int originalLimit = byteBuffer.limit();
      int newLimit = originalLimit;
      while (newLimit > byteBuffer.position() && byteBuffer.get(newLimit - 1) == 0) {
         newLimit--;
      }
      byteBuffer.limit(newLimit); // Removes trailing '\0' bytes

      try {
         document = XmlUtils.readDocument(byteBuffer);
      } catch (IOException e) {
         throw new DatagramFormatException(e);
      } finally {
         byteBuffer.limit(originalLimit);
      }

      byteBuffer.position(originalLimit);
   }

   public Document getDocument() {
      return document;
   }

   @Override
   public String toStringExtra() {
      return XmlUtils.toCompactString(document.getRootElement()).replaceAll("\\s+", " ");
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.put(XmlUtils.toDefaultBytes(document));
      byteBuffer.put((byte) 0);
   }
}
