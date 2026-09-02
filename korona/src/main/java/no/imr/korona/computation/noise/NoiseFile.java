package no.imr.korona.computation.noise;

import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.logging.Log;
import no.imr.tools.math.Quantile;
import no.imr.tools.math.QuickSelect;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class NoiseFile extends BaseNoiseFile {
   private static final long DEFAULT_INTERVAL_SECONDS = 60 * 60; // One hour

   private final RawFileConfiguration rawFileConfiguration;
   private final int maxNumberOfIntervals;
   private NoiseFileContent noiseFileContent;

   public NoiseFile(RawFileConfiguration rawFileConfiguration, @Nullable Path file, int maxNumberOfIntervals) {
      super(file);

      this.rawFileConfiguration = rawFileConfiguration;
      this.maxNumberOfIntervals = maxNumberOfIntervals;
      noiseFileContent = readNoiseFileContent();
      purgeOldEntries(noiseFileContent);
   }

   NavigableMap<Instant, NavigableMap<Integer, NoiseData>> getTimeToNoiseMap() {
      return noiseFileContent.timeToNoiseMap;
   }

   Document toXML() {
      return mergedNoiseDocument(null);
   }

   void fromXML(Document document) {
      noiseFileContent = parseNoiseDocument(document);
   }

   private NoiseFileContent readNoiseFileContent() {
      try {
         Document document = readNoiseFile();
         if (document != null) {
            return parseNoiseDocument(document);
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
      return new NoiseFileContent(DEFAULT_INTERVAL_SECONDS);
   }

   private static NoiseFileContent parseNoiseDocument(Document document) {
      Element rootElement = document.getRootElement();
      if (rootElement.getName().equalsIgnoreCase("noise")) { // Noise file with only one entry per frequency
         NoiseFileContent noiseFileContent = new NoiseFileContent(DEFAULT_INTERVAL_SECONDS);
         Instant date = Instant.EPOCH;
         NavigableMap<Integer, NoiseData> entries = new TreeMap<>();
         noiseFileContent.timeToNoiseMap.put(date, entries);
         parseNoiseEntries(entries, rootElement);
         return noiseFileContent;
      } else {
         NoiseFileContent noiseFileContent = new NoiseFileContent(DEFAULT_INTERVAL_SECONDS);
         long writeInterval = Long.parseLong(rootElement.attributeValue("intervalSeconds"));
         // Only read the noise values if the interval is the same
         if (writeInterval == DEFAULT_INTERVAL_SECONDS) {
            for (Element element : rootElement.elements()) {
               Instant date = Instant.parse(element.attributeValue("time"));
               NavigableMap<Integer, NoiseData> entries = new TreeMap<>();
               noiseFileContent.timeToNoiseMap.put(date, entries);
               parseNoiseEntries(entries, element);
            }
         }
         return noiseFileContent;
      }
   }

   private static void parseNoiseEntries(NavigableMap<Integer, NoiseData> entries, Element element) {
      for (Element entryElement : element.elements()) {
         int kHz = Integer.parseInt(entryElement.attributeValue("frequency"));
         float ne = Float.parseFloat(entryElement.attributeValue("NE"));
         float nh = Float.parseFloat(entryElement.attributeValue("NH"));
         float quality = Float.parseFloat(entryElement.attributeValue("quality"));
         entries.put(kHz, new NoiseData(ne, nh, quality));
      }
   }

   @Override
   protected Document mergedNoiseDocument(@Nullable Document documentOnFile) {
      NoiseFileContent mergedMap;
      if (documentOnFile == null) {
         mergedMap = noiseFileContent;
      } else {
         mergedMap = new NoiseFileContent(noiseFileContent.intervalSeconds);
         NavigableMap<Instant, NavigableMap<Integer, NoiseData>> mergedMapMap = mergedMap.timeToNoiseMap;
         noiseFileContent.timeToNoiseMap.forEach(mergedMapMap::putIfAbsent);
         NoiseFileContent fileNoiseFileContent = parseNoiseDocument(documentOnFile);
         if (fileNoiseFileContent.intervalSeconds == mergedMap.intervalSeconds) { // Only merge with values on file if the interval is the same.
            for (Map.Entry<Instant, NavigableMap<Integer, NoiseData>> entry : fileNoiseFileContent.timeToNoiseMap.entrySet()) {
               Instant date = entry.getKey();
               if (mergedMapMap.containsKey(date)) {
                  mergeNoiseMaps(entry.getValue(), mergedMapMap.get(date));
               } else {
                  mergedMapMap.put(date, entry.getValue());
               }
            }
         }
      }
      purgeOldEntries(mergedMap);
      return createNoiseDocument(mergedMap);
   }

   private static Document createNoiseDocument(NoiseFileContent noiseFileContent) {
      Element root = DocumentHelper.createElement("noiseHistory")
            .addAttribute("intervalSeconds", Long.toString(noiseFileContent.intervalSeconds));
      for (Map.Entry<Instant, NavigableMap<Integer, NoiseData>> timeEntry : noiseFileContent.timeToNoiseMap.entrySet()) {
         Instant date = timeEntry.getKey();
         Element noiseElement = root.addElement("noise")
               .addAttribute("time", date.toString());
         for (Map.Entry<Integer, NoiseData> entry : timeEntry.getValue().entrySet()) {
            Integer kHz = entry.getKey();
            NoiseData noiseData = entry.getValue();
            noiseElement.addElement("entry")
                  .addAttribute("frequency", Integer.toString(kHz))
                  .addAttribute("NE", Float.toString(noiseData.ne))
                  .addAttribute("NH", Float.toString(noiseData.nh))
                  .addAttribute("quality", Float.toString(noiseData.quality));
         }
      }
      return DocumentHelper.createDocument(root);
   }

   private static void mergeNoiseMaps(NavigableMap<Integer, NoiseData> parsedMap, NavigableMap<Integer, NoiseData> mergedMap) {
      for (Map.Entry<Integer, NoiseData> fileEntry : parsedMap.entrySet()) {
         Integer kHz = fileEntry.getKey();
         if (!mergedMap.containsKey(kHz)) {
            mergedMap.put(kHz, fileEntry.getValue());
         } else {
            NoiseData fileNoiseData = fileEntry.getValue();
            NoiseData internalNoiseData = mergedMap.get(kHz);
            NoiseData mergedNoiseData = fileNoiseData.isBetterThan(internalNoiseData) ? fileNoiseData : internalNoiseData;
            mergedMap.put(kHz, mergedNoiseData);
         }
      }
   }

   private int channelToKHz(int channel) {
      return rawFileConfiguration.getTransducers().get(channel - 1).getKHz();
   }

   void addFallbackNqp0Datagram(short channel, Ping ping) {
      Integer kHz = channelToKHz(channel);
      Map.Entry<Instant, NavigableMap<Integer, NoiseData>> entry = getClosestMapEntry(ping.getInstant(), kHz);
      if (entry == null) {
         return;
      }
      NavigableMap<Integer, NoiseData> noiseMap = entry.getValue();
      NoiseData noiseData = noiseMap.get(kHz);
      if (noiseData != null) {
         ping.add(new Nqp0Datagram(ping.getInstant(), channel,
               noiseData.ne, noiseData.nh, 10 + noiseData.quality / 10));
      }
   }

   void update(NoiseData noiseData, int channel, Instant instant) {
      Map.Entry<Instant, NavigableMap<Integer, NoiseData>> entry = noiseFileContent.timeToNoiseMap.floorEntry(instant);
      if (entry == null || !instant.isBefore(entry.getKey().plusSeconds(noiseFileContent.intervalSeconds))) {
         Instant time = Instant.ofEpochSecond((instant.getEpochSecond() / noiseFileContent.intervalSeconds) * noiseFileContent.intervalSeconds);
         noiseFileContent.timeToNoiseMap.put(time, new TreeMap<>());
         entry = noiseFileContent.timeToNoiseMap.floorEntry(instant);
      }
      purgeOldEntries(noiseFileContent);

      NavigableMap<Integer, NoiseData> noiseMap = entry.getValue();
      Integer key = channelToKHz(channel);
      NoiseData existingNoiseData = noiseMap.get(key);
      if (existingNoiseData == null || noiseData.isBetterThan(existingNoiseData)) {
         noiseMap.put(key, noiseData);
         writeNoiseFile();
      }
   }

   private Map.@Nullable Entry<Instant, NavigableMap<Integer, NoiseData>> getClosestMapEntry(Instant instant, Integer kHz) {
      Map.Entry<Instant, NavigableMap<Integer, NoiseData>> floorEntry = noiseFileContent.timeToNoiseMap.floorEntry(instant);
      while (floorEntry != null && !floorEntry.getValue().containsKey(kHz)) {
         floorEntry = noiseFileContent.timeToNoiseMap.lowerEntry(floorEntry.getKey());
      }
      long floorDist;
      if (floorEntry != null) {
         Instant endTime = floorEntry.getKey().plusSeconds(noiseFileContent.intervalSeconds);
         if (instant.isBefore(endTime)) {
            // Time is inside this noise interval.
            return floorEntry;
         }
         floorDist = endTime.until(instant, ChronoUnit.NANOS);
      } else {
         floorDist = Long.MAX_VALUE;
      }
      Map.Entry<Instant, NavigableMap<Integer, NoiseData>> ceilEntry = noiseFileContent.timeToNoiseMap.ceilingEntry(instant);
      while (ceilEntry != null && !ceilEntry.getValue().containsKey(kHz)) {
         ceilEntry = noiseFileContent.timeToNoiseMap.higherEntry(ceilEntry.getKey());
      }
      long ceilDist;
      if (ceilEntry != null) {
         ceilDist = instant.until(ceilEntry.getKey(), ChronoUnit.NANOS);
      } else {
         ceilDist = Long.MAX_VALUE;
      }
      return floorDist <= ceilDist ? floorEntry : ceilEntry;
   }

   private void purgeOldEntries(NoiseFileContent noiseFileContent) {
      if (noiseFileContent.timeToNoiseMap.size() > maxNumberOfIntervals) {
         List<Instant> descendingTimes = new ArrayList<>(noiseFileContent.timeToNoiseMap.descendingKeySet());
         noiseFileContent.timeToNoiseMap.descendingMap().tailMap(descendingTimes.get(maxNumberOfIntervals)).clear();
      }
   }

   public NavigableMap<Integer, NoiseData> timeToNoiseMap(Instant instant) {
      Map.Entry<Instant, NavigableMap<Integer, NoiseData>> entry = noiseFileContent.timeToNoiseMap.floorEntry(instant);
      if (entry == null || !instant.isBefore(entry.getKey().plusSeconds(noiseFileContent.intervalSeconds))) {
         return Collections.emptyNavigableMap();
      }
      return entry.getValue();
   }

   @Nullable NoiseData findQuantileNoiseData(int channel, float quantile) {
      Integer key = channelToKHz(channel);
      List<NoiseData> list = noiseFileContent.timeToNoiseMap.values().stream()
            .map(noiseMap -> noiseMap.get(key))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
      if (list.isEmpty()) {
         return null;
      }
      int targetIndex = Quantile.quantileIndex(quantile, list.size());
      return QuickSelect.get(list, Comparator.comparingDouble(NoiseData::ne), targetIndex);
   }

   public record NoiseData(
         float ne, // Average
         float nh, // Upper limit
         float quality
   ) {
      private boolean isBetterThan(NoiseData other) {
         return quality > other.quality ||                // Higher quality, or
               quality == other.quality && ne < other.ne; // same quality and lower noise.
      }
   }

   private static final class NoiseFileContent {
      private final NavigableMap<Instant, NavigableMap<Integer, NoiseData>> timeToNoiseMap = new TreeMap<>();
      private final long intervalSeconds;

      private NoiseFileContent(long intervalSeconds) {
         this.intervalSeconds = intervalSeconds;
      }
   }
}
