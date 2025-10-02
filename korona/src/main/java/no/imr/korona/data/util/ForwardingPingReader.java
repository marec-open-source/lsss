package no.imr.korona.data.util;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingReader;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public abstract class ForwardingPingReader implements PingReader {
   private final PingReader pingReader;

   protected ForwardingPingReader(PingReader pingReader) {
      this.pingReader = pingReader;
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
      return pingReader.getPingConfiguration();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      return pingReader.nextPing(asyncHandle);
   }
}
