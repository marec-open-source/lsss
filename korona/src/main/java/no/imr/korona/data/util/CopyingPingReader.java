package no.imr.korona.data.util;

import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class CopyingPingReader implements PingReader {
   private final PingReader pingReader;
   private final PingConfiguration copiedPingConfiguration;

   public CopyingPingReader(PingReader pingReader) {
      this.pingReader = pingReader;

      List<PingItem> copiedConfigurationItems = pingReader.getPingConfiguration().getConfigurationItems().stream()
            .map(PingItem::makeCopy)
            .collect(Collectors.toList());
      copiedPingConfiguration = new PingConfiguration(copiedConfigurationItems);
   }

   @Override
   public Path getFile() {
      return pingReader.getFile();
   }

   @Override
   public float getReadFraction() {
      return pingReader.getReadFraction();
   }

   @Override
   public void close() throws IOException {
      pingReader.close();
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return copiedPingConfiguration;
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      Ping ping = pingReader.nextPing(asyncHandle);
      if (ping == null) {
         return null;
      }
      Ping copiedPing = new DefaultPing(
            copiedPingConfiguration,
            new DefaultPingIndex(ping.getPingIndex()),
            ping.getBot0Datagram().makeCopy());
      ping.getPingItems().stream()
            .map(PingItem::makeCopy)
            .forEach(copiedPing::add);
      return copiedPing;
   }
}
