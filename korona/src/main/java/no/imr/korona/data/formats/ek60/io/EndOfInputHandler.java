package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.util.KoronaUtils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Decides how long to wait for more data when a {@link ChannelDatagramReader} reaches end of input.
 */
@FunctionalInterface
public interface EndOfInputHandler {
   boolean isEndOfInput(long millisWaiting, Comparator<FileInfo> comparator) throws IOException;

   static EndOfInputHandler noWait() {
      return (millisWaiting, comparator) -> true;
   }

   /**
    * Waits indefinitely for next file in same directory.
    */
   final class NextFileWait implements EndOfInputHandler {
      private final Path file;
      private final AsyncHandle asyncHandle;
      private long waitIndex;

      public NextFileWait(Path file, AsyncHandle asyncHandle) {
         this.file = file;
         this.asyncHandle = asyncHandle;
      }

      @Override
      public boolean isEndOfInput(long millisWaiting, Comparator<FileInfo> comparator) throws IOException {
         // Wait a little bit since finding next raw requires listing potentially many files
         long previousWaitIndex = waitIndex;
         waitIndex = millisWaiting / 2000;
         if (waitIndex > previousWaitIndex && KoronaUtils.nextRawFile(file.getParent(), file, comparator) != null) {
            return true;
         } else {
            asyncHandle.sleep(500);
            return asyncHandle.isCancelled();
         }
      }
   }
}
