package no.imr.korona.data.util;

import no.imr.korona.data.DataException;
import no.imr.korona.data.DataFormatManager;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentHandle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Factory for creating a {@link PingReader}.
 */
public final class PingReaderFactory {
   private PingReaderFactory() {
   }

   public static PingReader create(DataFormatManager dataFormatManager, Path file) throws IOException {
      return create(dataFormatManager, file, false);
   }

   public static PingReader create(DataFormatManager dataFormatManager, Path file, boolean recursive) throws IOException {
      if (Files.isDirectory(file)) {
         return new DirectoryPingReader(dataFormatManager, file, recursive);
      } else {
         SegmentHandle segmentHandle = dataFormatManager.createSegmentHandle(file);
         if (segmentHandle == null) {
            if (Files.exists(file)) {
               throw new DataException("Unknown data format: " + file);
            } else {
               throw new NoSuchFileException("File not found: " + file);
            }
         }
         return segmentHandle.createPingReader();
      }
   }
}
