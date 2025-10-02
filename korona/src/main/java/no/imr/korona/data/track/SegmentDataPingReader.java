package no.imr.korona.data.track;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingReader;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * A {@link PingReader} implemented using a {@link SegmentData}.
 */
public final class SegmentDataPingReader implements PingReader {
   private final SegmentData segmentData;
   private final Path file;
   private int index;

   public SegmentDataPingReader(SegmentData segmentData, Path file) {
      this.segmentData = segmentData;
      this.file = file;
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      List<? extends PingIndex> pingIndices = segmentData.getPingIndices();
      if (index < pingIndices.size()) {
         Ping ping = segmentData.loadPing(pingIndices.get(index++), asyncHandle);
         return asyncHandle.isCancelled() ? null : ping;
      } else {
         return null;
      }
   }

   @Override
   public Path getFile() {
      return file;
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return segmentData.getPingConfiguration();
   }

   @Override
   public float getReadFraction() {
      return (float) index / segmentData.getPingIndices().size();
   }

   @Override
   public void close() throws IOException {
      index = segmentData.getPingIndices().size();
      segmentData.close();
   }
}
