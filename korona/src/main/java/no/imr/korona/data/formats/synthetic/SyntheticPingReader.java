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
public final class SyntheticPingReader implements PingReader {
   private final Path file;
   private final SyntheticData syntheticData;
   private long pingNumber;

   public SyntheticPingReader(SyntheticData syntheticData) {
      this(new SyntheticDataFile(syntheticData));
   }

   SyntheticPingReader(SyntheticDataFile syntheticDataFile) {
      file = syntheticDataFile.getFile();
      syntheticData = syntheticDataFile.getSyntheticData();
      pingNumber = syntheticData.getFirstPingNumber();
   }

   @Override
   public Path getFile() {
      return file;
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return syntheticData.getPingConfiguration();
   }

   @Override
   public float getReadFraction() {
      return (float) (pingNumber - syntheticData.getFirstPingNumber()) / (float) syntheticData.getPingCount();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) {
      if (pingNumber > syntheticData.getLastPingNumber()) {
         return null;
      }
      PingIndex pingIndex = syntheticData.createPingIndex(pingNumber++);
      return syntheticData.createPing(pingIndex);
   }

   @Override
   public void close() {
      pingNumber = syntheticData.getLastPingNumber() + 1;
   }
}
