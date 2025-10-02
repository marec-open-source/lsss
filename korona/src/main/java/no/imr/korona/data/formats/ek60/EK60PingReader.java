package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.formats.ek60.io.RealtimeFileDatagramReader;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndexCorrectionOptions;
import no.imr.korona.data.ping.PingReader;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for EK60 ping readers.
 */
public abstract class EK60PingReader implements PingReader {
   EK60PingReader() {
   }

   @Override
   public float getReadFraction() {
      return (float) getEndOffset() / (float) FileUtils.sizeOr0(getFile());
   }

   public static EK60PingReader create(Path file, EndOfInputHandler endOfInputHandler, DatagramTypeManager datagramTypeManager, boolean realtime) throws IOException {
      return create(new EK60FileSet(file), endOfInputHandler, datagramTypeManager, realtime);
   }

   public static EK60PingReader createRawOnly(Path file, EndOfInputHandler endOfInputHandler, DatagramTypeManager datagramTypeManager, boolean realtime) throws IOException {
      return new EK60PingReaderRawOnly(new EK60FileSet(file).getRaw(), endOfInputHandler, 1, new PingIndexCorrectionOptions(), datagramTypeManager, realtime);
   }

   public static EK60PingReader create(EK60FileSet ek60FileSet, EndOfInputHandler endOfInputHandler, DatagramTypeManager datagramTypeManager, boolean realtime) throws IOException {
      if (IdxFile.useIdxFiles && Files.exists(ek60FileSet.getIdx())) {
         return new EK60PingReaderFileSet(ek60FileSet, endOfInputHandler, datagramTypeManager, realtime);
      } else {
         return new EK60PingReaderRawOnly(ek60FileSet.getRaw(), endOfInputHandler, 1, new PingIndexCorrectionOptions(), datagramTypeManager, realtime);
      }
   }

   static FileDatagramReader newFileDatagramReader(Path file, DatagramTypeManager datagramTypeManager, boolean realtime) throws IOException {
      return realtime ? new RealtimeFileDatagramReader(file, datagramTypeManager) : new FileDatagramReader(file, datagramTypeManager);
   }

   /**
    * Returns the maximum file offset corresponding to the pings that have been returned by {@link #nextPing(AsyncHandle)})}.
    * Note that the raw file may actually be longer if it is concurrently being written to.
    *
    * @return the max file offset
    */
   public abstract long getEndOffset();

   public abstract long getPosition() throws IOException;

   public abstract void skipToEnd(AsyncHandle asyncHandle) throws IOException;

   public abstract @Nullable Ping nextEmptyPing(AsyncHandle asyncHandle) throws IOException;

   public void disallowPingIndexBuffering() {
   }
}
