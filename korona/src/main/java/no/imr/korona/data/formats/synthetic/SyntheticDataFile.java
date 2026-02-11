package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.DataException;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A file that represents synthetic data.
 * File name syntax:
 * <pre>{@code
 * <class name>[@<parameter name>=<value>]*[@<first ping number>-<last ping number>].lsss-ss
 * }</pre>
 * Ping range is {@code [<first ping number>, <last ping number> + 1)}.
 *
 * @see SyntheticData
 */
public final class SyntheticDataFile {
   private final SyntheticData syntheticData;
   private final long firstPingNumber;
   private int pingCount;
   private @Nullable PingConfiguration pingConfiguration;

   public SyntheticDataFile(SyntheticData syntheticData, long firstPingNumber, int pingCount) {
      this.syntheticData = syntheticData;
      this.firstPingNumber = firstPingNumber;
      this.pingCount = pingCount;
   }

   SyntheticData getSyntheticData() {
      return syntheticData;
   }

   public long getFirstPingNumber() {
      return firstPingNumber;
   }

   public long getLastPingNumber() {
      return firstPingNumber + pingCount - 1;
   }

   public int getPingCount() {
      return pingCount;
   }

   public void setLastPingNumber(long lastPingNumber) {
      pingCount = (int) (lastPingNumber - firstPingNumber + 1);
   }

   public PingRange toPingRange() {
      PingIndex begin = createPingIndex(firstPingNumber);
      PingIndex end = createPingIndex(firstPingNumber + pingCount);
      return PingRange.of(begin, end);
   }

   public PingConfiguration getPingConfiguration() {
      PingConfiguration result = pingConfiguration;
      if (result == null) {
         result = syntheticData.createPingConfiguration(this);
         pingConfiguration = result;
      }
      return result;
   }

   public RawFileConfiguration getRawFileConfiguration() {
      return getPingConfiguration().getRawFileConfiguration();
   }

   public PingIndex createPingIndex(long pingNumber) {
      return SyntheticFactory.createPingIndex(syntheticData, pingNumber);
   }

   Bot0Datagram createBot0Datagram(PingIndex pingIndex) {
      return SyntheticFactory.createBot0Datagram(syntheticData, pingIndex);
   }

   public Ping createPing(PingIndex pingIndex) {
      return new DefaultPing(pingIndex, createBot0Datagram(pingIndex), createPingData(pingIndex));
   }

   PingData createPingData(PingIndex pingIndex) {
      PingData pingData = new PingData(getPingConfiguration());
      for (int channel = 1; channel <= getRawFileConfiguration().getTransducerCount(); channel++) {
         PowerData powerData = createPowerData(pingIndex, channel);
         if (powerData != null) {
            pingData.add(powerData);
         }
      }
      syntheticData.addOtherDatagrams(pingIndex, pingData);
      return pingData;
   }

   public @Nullable PowerData createPowerData(PingIndex pingIndex, int channel) {
      return syntheticData.hasPowerData(pingIndex, channel)
            ? SyntheticFactory.createPowerData(this, pingIndex, channel)
            : null;
   }

   public SegmentHandle toSegmentHandle() {
      return new SyntheticSegmentHandle(toFile(), this);
   }


   public PingReader toPingReader() {
      return new SyntheticPingReader(toFile(), this);
   }

   static SyntheticDataFile toSyntheticData(Path file) throws DataException {
      String name = file.getFileName().toString();
      if (!name.endsWith(SyntheticDataFormatPlugin.LSSS_SS_SUFFIX)) {
         throw new DataException("Unknown suffix: " + file);
      }
      name = name.substring(0, name.length() - SyntheticDataFormatPlugin.LSSS_SS_SUFFIX.length());
      String[] parts = name.split("@");
      try {
         String className = parts[0];
         Class<? extends SyntheticData> clazz = Class.forName(className).asSubclass(SyntheticData.class);
         SyntheticData syntheticData = clazz.getDeclaredConstructor().newInstance();

         long firstPingNumber = 1;
         long lastPingNumber = 100000;

         for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.matches(".+=.+")) {
               String[] nameAndVal = part.split("=", 2);
               syntheticData.setParameter(nameAndVal[0], nameAndVal[1]);
            } else if (part.matches("\\d+-\\d+")) {
               String[] range = part.split("-");
               firstPingNumber = Long.parseLong(range[0]);
               lastPingNumber = Long.parseLong(range[1]);
            } else {
               throw new DataException("Cannot parse " + part + " in " + file);
            }
         }
         return syntheticData.withFirstAndLastPingNumber(firstPingNumber, lastPingNumber);
      } catch (Exception e) {
         throw new DataException("Error creating data definition: " + file, e);
      }
   }

   public Path toFile() {
      return toFile(Utils.getTmpDir());
   }

   public Path toFile(Path dir) {
      StringBuilder fileName = new StringBuilder(syntheticData.getClass().getName());
      Map<String, String> parameters = new LinkedHashMap<>();
      syntheticData.addParameters(parameters);
      parameters.forEach((key, value) -> {
         fileName.append("@" + key + "=" + value);
      });
      fileName.append("@" + getFirstPingNumber() + "-" + getLastPingNumber() + SyntheticDataFormatPlugin.LSSS_SS_SUFFIX);
      return dir.resolve(fileName.toString());
   }
}
