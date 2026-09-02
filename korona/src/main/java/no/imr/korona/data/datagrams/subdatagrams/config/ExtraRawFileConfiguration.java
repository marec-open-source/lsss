package no.imr.korona.data.datagrams.subdatagrams.config;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * For extra configuration not in XML0/Configuration, XML0/Environment.
 */
public final class ExtraRawFileConfiguration extends BaseSubDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.EXTRA_RAW_FILE_CONFIGURATION,
         "Extra raw file configuration", ExtraRawFileConfiguration::new);

   public static final String XML_ROOT = "ExtraConfiguration";

   private final Element element;

   public ExtraRawFileConfiguration(Instant instant, Element element) {
      super(instant);

      this.element = element;
   }

   private ExtraRawFileConfiguration(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      try {
         element = XmlUtils.readDocument(byteBuffer).getRootElement();
      } catch (IOException e) {
         throw new DatagramFormatException(e);
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.put(XmlUtils.toDefaultBytes(element));
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   public Element getElement() {
      return element;
   }
}
