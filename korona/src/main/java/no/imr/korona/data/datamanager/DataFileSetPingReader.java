package no.imr.korona.data.datamanager;

import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingReader;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class DataFileSetPingReader implements PingReader {
   private final DataFileSet dataFileSet;
   private final PingRange pingRange;
   private final PingConfiguration pingConfiguration;
   private PingIndex nextPingIndex;

   public DataFileSetPingReader(DataFileSet dataFileSet, PingRange pingRange) {
      this.dataFileSet = dataFileSet;
      this.pingRange = pingRange;
      pingConfiguration = dataFileSet.getDataFile(pingRange.begin()).getPingConfiguration();
      nextPingIndex = pingRange.begin();
   }

   @Override
   public Path getFile() {
      return dataFileSet.getDataFile(pingRange.begin()).getSegmentHandle().getMainFile();
   }

   @Override
   public float getReadFraction() {
      return (float) (nextPingIndex.getPingNumber() - pingRange.begin().getPingNumber()) / (float) pingRange.getPingCount();
   }

   @Override
   public void close() {
      nextPingIndex = pingRange.end();
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) {
      if (!pingRange.contains(nextPingIndex)) {
         return null;
      }
      Ping ping = dataFileSet.getPing(nextPingIndex);
      nextPingIndex = dataFileSet.nextOrSame(nextPingIndex);

      if (ping.getPingConfiguration() != pingConfiguration) {
         // This happens if the ping range spans more than one file
         Ping convertedPing = new DefaultPing(pingConfiguration, ping.getPingIndex(), ping.getBot0Datagram());
         convertedPing.addAll(ping.getPingItems());
         ping = convertedPing;
      }

      return ping;
   }
}
