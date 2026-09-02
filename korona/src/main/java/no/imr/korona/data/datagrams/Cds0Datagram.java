package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Module configuration.
 */
public final class Cds0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("CDS0", Cds0Datagram::new);

   private final Document document;

   public Cds0Datagram(Instant instant, Document document) {
      super(instant);

      this.document = document;
   }

   public Cds0Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      int length = ByteBufferUtils.readCount(byteBuffer, 1);
      byte[] bytes = new byte[length];
      byteBuffer.get(bytes);

      try {
         document = XmlUtils.readDocument(bytes);
      } catch (IOException e) {
         throw new DatagramFormatException(e);
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byte[] bytes = XmlUtils.toDefaultBytes(document);
      byteBuffer.putInt(bytes.length);
      byteBuffer.put(bytes);
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
}
