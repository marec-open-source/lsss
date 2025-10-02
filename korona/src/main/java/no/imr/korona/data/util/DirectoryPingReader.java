package no.imr.korona.data.util;

import no.imr.korona.data.DataFormatManager;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads all files in a directory.
 * Can also traverse subdirectories.
 */
public final class DirectoryPingReader implements PingReader {
   private final List<SegmentHandle> segmentHandles;
   private PingReader pingReader;
   private int nextFileIndex;

   public DirectoryPingReader(DataFormatManager dataFormatManager, Path directory, boolean recursive) throws IOException {
      if (recursive) {
         segmentHandles = dataFormatManager.createSegmentHandlesInDirectoryRecursively(directory, new AsyncHandle());
      } else {
         segmentHandles = dataFormatManager.createSegmentHandlesInDirectory(directory);
      }
      if (segmentHandles.isEmpty()) {
         throw new IOException("Found no data in " + directory);
      }
      pingReader = segmentHandles.getFirst().createPingReader();
      nextFileIndex = 1;
   }

   @Override
   public Path getFile() {
      return pingReader.getFile();
   }

   @Override
   public float getReadFraction() {
      return (nextFileIndex - 1 + pingReader.getReadFraction()) / segmentHandles.size();
   }

   @Override
   public void close() throws IOException {
      pingReader.close();
      nextFileIndex = segmentHandles.size();
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingReader.getPingConfiguration();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      Ping ping = pingReader.nextPing(asyncHandle);
      if (ping == null) {
         openNextFile();
         ping = pingReader.nextPing(asyncHandle);
      }
      return ping;
   }

   private void openNextFile() throws IOException {
      while (nextFileIndex < segmentHandles.size()) {
         SegmentHandle nextSegmentHandle = segmentHandles.get(nextFileIndex);
         nextFileIndex++;
         PingReader nextPingReader = nextSegmentHandle.createPingReader();
         String incompatibility = pingReader.getPingConfiguration().getIncompatibility(nextPingReader.getPingConfiguration());
         if (incompatibility != null) {
            Log.global.warning("File " + nextPingReader.getFile() + " is incompatible: " + incompatibility);
            nextPingReader.close();
         } else {
            pingReader.close();
            pingReader = nextPingReader;
            return;
         }
      }
   }
}
