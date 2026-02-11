package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingReader;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * A synthetic ping reader.
 */
final class SyntheticPingReader implements PingReader {
   private final Path file;
   private final SyntheticDataFile syntheticDataFile;
   private long pingNumber;

   SyntheticPingReader(Path file, SyntheticDataFile syntheticDataFile) {
      this.file = file;
      this.syntheticDataFile = syntheticDataFile;
      pingNumber = syntheticDataFile.getFirstPingNumber();
   }

   @Override
   public Path getFile() {
      return file;
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return syntheticDataFile.getPingConfiguration();
   }

   @Override
   public float getReadFraction() {
      return (float) (pingNumber - syntheticDataFile.getFirstPingNumber()) / (float) syntheticDataFile.getPingCount();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) {
      if (pingNumber > syntheticDataFile.getLastPingNumber()) {
         return null;
      }
      PingIndex pingIndex = syntheticDataFile.createPingIndex(pingNumber++);
      return syntheticDataFile.createPing(pingIndex);
   }

   @Override
   public void close() {
      pingNumber = syntheticDataFile.getLastPingNumber() + 1;
   }
}
