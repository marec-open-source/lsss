package no.imr.korona.data.ping.items;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.MruDatagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class PingConversion {
   private final Path file;
   private final @Nullable PingConfiguration pingConfiguration;
   private final List<BaseDatagram> datagrams;
   private final Supplier<@Nullable MruDatagram> missingMruProvider;
   private final Map<String, Element> idToChannelParameter;

   private @Nullable String dateAndFileString;
   private @Nullable MruDatagram mruDatagram;
   private @Nullable Map<String, Integer> idToChannel;

   private final List<PingItem> pingItems;

   public PingConversion(Path file, List<BaseDatagram> datagrams) {
      this.file = file;
      pingConfiguration = null;
      this.datagrams = datagrams;
      missingMruProvider = () -> null;
      idToChannelParameter = Map.of();
      pingItems = new ArrayList<>(datagrams.size());
      datagrams.forEach(datagram -> datagram.addPingItems(this));
   }

   public PingConversion(Path file, PingConfiguration pingConfiguration, List<BaseDatagram> datagrams, Supplier<@Nullable MruDatagram> missingMruProvider) {
      this.file = file;
      this.pingConfiguration = pingConfiguration;
      this.datagrams = datagrams;
      this.missingMruProvider = missingMruProvider;
      idToChannelParameter = HashMap.newHashMap(pingConfiguration.getRawFileConfiguration().getTransducerCount());
      List<BaseDatagram> datagramsToConvert = new ArrayList<>(datagrams.size());
      datagrams.forEach(datagram -> {
         if (datagram instanceof Xml0Datagram xml0Datagram) {
            Element rootElement = xml0Datagram.getDocument().getRootElement();
            if (rootElement.getName().equals("Parameter")) {
               rootElement.elements().forEach(element -> {
                  String channelId = element.attributeValue("ChannelID");
                  Element existingElement = idToChannelParameter.putIfAbsent(channelId, element);
                  if (existingElement != null && !XmlUtils.equalContent(existingElement, element)) {
                     Log.global.log(Log.SILENT_WARNING, "Incompatible duplicate XML0/Parameter/Channel for " + channelId + " " + getDateAndFileString());
                  }
               });
               return;
            }
         }
         datagramsToConvert.add(datagram);
      });
      pingItems = new ArrayList<>(datagramsToConvert.size());
      datagramsToConvert.forEach(datagram -> datagram.addPingItems(this));
   }

   public PingConfiguration getPingConfiguration() {
      PingConfiguration pingConfiguration = this.pingConfiguration;
      if (pingConfiguration == null) {
         throw new IllegalStateException();
      }
      return pingConfiguration;
   }

   public String getDateAndFileString() {
      if (dateAndFileString == null) {
         BaseDatagram datagram = datagrams.stream()
               .filter(BaseDatagram::isSampleDatagram)
               .findFirst()
               .orElse(datagrams.isEmpty() ? null : datagrams.getFirst());
         String dateString = datagram != null ? datagram.getInstant().toString() : "<no date>";
         dateAndFileString = dateString + ", " + file;
      }
      return dateAndFileString;
   }

   public @Nullable MruDatagram getMruDatagram() {
      MruDatagram mruDatagram = this.mruDatagram;
      if (mruDatagram == null) {
         mruDatagram = Utils.getFirstOrNull(datagrams, MruDatagram.class);
         if (mruDatagram == null) {
            mruDatagram = missingMruProvider.get();
         }
         this.mruDatagram = mruDatagram;
      }
      return mruDatagram;
   }

   public Map<String, Element> getIdToChannelParameter() {
      return idToChannelParameter;
   }

   public Map<String, Integer> getIdToChannel() {
      Map<String, Integer> idToChannel = this.idToChannel;
      if (idToChannel == null) {
         List<RawFileTransducer> transducers = getPingConfiguration().getRawFileConfiguration().getTransducers();
         idToChannel = HashMap.newHashMap(transducers.size());
         for (int channel = 1; channel <= transducers.size(); channel++) {
            idToChannel.put(transducers.get(channel - 1).getChannelId(), channel);
         }
         this.idToChannel = idToChannel;
      }
      return idToChannel;
   }

   public void addPingItem(PingItem pingItem) {
      pingItems.add(pingItem);
   }

   public List<PingItem> getPingItems() {
      return pingItems;
   }
}
