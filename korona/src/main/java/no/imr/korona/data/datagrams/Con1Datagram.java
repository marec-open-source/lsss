package no.imr.korona.data.datagrams;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.ping.items.PassivePingItem;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.Utils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.Node;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Sonar configuration datagram.
 */
public final class Con1Datagram extends BaseDatagram {
   public static final DatagramType TYPE = DatagramType.simple("CON1", Con1Datagram::new);

   private static ImmutableMap<String, Function<Con1Datagram, @Nullable PingItem>> pingItemConverters = ImmutableMap.of();

   private final byte[] bytes;
   private final Document document;

   /**
    * Constructs a Con1Datagram given a time a ByteBuffer to read from.
    *
    * @param instant    time for datagram
    * @param byteBuffer buffer to get from
    * @throws DatagramFormatException when parsing fails
    */
   public Con1Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      bytes = new byte[byteBuffer.remaining()];
      byteBuffer.get(bytes);
      try {
         // Do not use the last byte if null terminated.
         int length = bytes.length;
         if (length > 0 && bytes[length - 1] == 0) {
            length--;
         }
         document = XmlUtils.readDocument(new String(bytes, 0, length, Utils.ISO_8859_1)); // Yngve: For reading CON1 datagrams with wrong encoding.
      } catch (IOException e) {
         throw new DatagramFormatException("Error parsing con1", e);
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.put(bytes);
   }

   public static synchronized void addPingItemConverter(Function<Con1Datagram, @Nullable PingItem> pingItemConverter, String sounderName) {
      pingItemConverters = ImmutableUtils.put(pingItemConverters, sounderName, pingItemConverter);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      boolean noneAdded = true;
      String sounderName = getSounderName(pingConversion.getPingItems());
      Function<Con1Datagram, @Nullable PingItem> pingItemConverter = pingItemConverters.get(sounderName);
      if (pingItemConverter != null) {
         PingItem pingItem = pingItemConverter.apply(this);
         if (pingItem != null) {
            pingConversion.addPingItem(pingItem);
            noneAdded = false;
         }
      }
      if (noneAdded) {
         pingConversion.addPingItem(new PassivePingItem(this));
      }
   }

   private static @Nullable String getSounderName(List<PingItem> configurationItems) {
      RawFileConfiguration rawFileConfiguration = Utils.getFirstOrNull(configurationItems, RawFileConfiguration.class);
      if (rawFileConfiguration != null) {
         return rawFileConfiguration.getSounderName();
      }
      for (PingItem configurationItem : configurationItems) {
         if (configurationItem instanceof Xml0Datagram xml0Datagram) {
            Node node = xml0Datagram.getDocument().selectSingleNode("/Configuration/Header/@ApplicationName");
            if (node != null) {
               return node.getText();
            }
         }
      }
      return null;
   }

   public Document getDocument() {
      return document;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public String toStringExtra() {
      return XmlUtils.toCompactString(document.getRootElement()).replaceAll("\\s+", " ");
   }
}
