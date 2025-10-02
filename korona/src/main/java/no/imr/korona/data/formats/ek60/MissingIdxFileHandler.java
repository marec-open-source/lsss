package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.ping.PingIndexCorrectionOptions;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public final class MissingIdxFileHandler {
   private static @Nullable DatagramTypeManager datagramTypeManager;

   private MissingIdxFileHandler() {
   }

   public static void setDatagramTypeManager(DatagramTypeManager datagramTypeManager) {
      MissingIdxFileHandler.datagramTypeManager = datagramTypeManager;
   }

   static IdxFile load(Path file) throws IOException {
      if (datagramTypeManager == null) {
         throw new MissingIdxFileException("Missing idx file: " + file);
      }
      Log.global.info("Creating in memory missing idx file: " + file);
      IdxFile idxFile = EK60Utils.createIdxFile(datagramTypeManager, new EK60FileSet(file), 1, new AsyncHandle(), ProgressHandler.ignore(),
            new PingIndexCorrectionOptions(true, true), false);
      assert idxFile != null;
      return idxFile;
   }
}
