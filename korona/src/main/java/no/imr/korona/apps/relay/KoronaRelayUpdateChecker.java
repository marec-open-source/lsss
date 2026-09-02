package no.imr.korona.apps.relay;

import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.io.LockedFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Class for checking for .new.raw files from KoronaRelay, ready for copy to .raw.
 */
public final class KoronaRelayUpdateChecker {
   private final Path statusFile;
   private long previousLastModified;

   public KoronaRelayUpdateChecker(Path directory) {
      statusFile = directory.resolve(KoronaRelay.STATUS_FILENAME);
   }

   public Path getDirectory() {
      return statusFile.getParent();
   }

   public Path getStatusFile() {
      return statusFile;
   }

   /**
    * Creates an UpdateLoader for loading the content of status.xml.
    * <p>
    * The UpdateLoader gets a lock on the directory, to prevent updates to files while they are being copied.
    * Make sure that {@link UpdateLoader#close()} is called when copy is done.
    *
    * @param forceReadStatusFile status file read if forceReadStatusFile is true or status file is modified since last time
    * @return an UpdateLoader
    * @throws IOException if some IO error occurs
    */
   public UpdateLoader createUpdateLoader(boolean forceReadStatusFile) throws IOException {
      BasicFileAttributes attributes = FileUtils.readAttributesIfExists(statusFile);
      if (attributes == null) {
         return new EmptyUpdateLoader();
      }

      long lastModified = attributes.lastModifiedTime().toMillis();
      if (forceReadStatusFile || previousLastModified < lastModified) {
         previousLastModified = lastModified;
         return new StatusFileUpdateLoader(statusFile);
      }

      return new EmptyUpdateLoader();
   }

   public interface UpdateLoader extends AutoCloseable {
      List<KoronaRelayUpdate> getUpdates();

      void saveStatus() throws IOException;

      @Override
      void close() throws IOException;
   }

   private static final class EmptyUpdateLoader implements UpdateLoader {
      private EmptyUpdateLoader() {
      }

      @Override
      public List<KoronaRelayUpdate> getUpdates() {
         return List.of();
      }

      @Override
      public void saveStatus() {
      }

      @Override
      public void close() {
      }
   }

   private static final class StatusFileUpdateLoader implements UpdateLoader {
      private final LockedFile lockedFile;
      private final KoronaRelayStatus koronaRelayStatus;

      private StatusFileUpdateLoader(Path statusFile) throws IOException {
         Path lockFile = statusFile.resolveSibling(KoronaRelay.STATUS_LOCK_FILENAME);
         lockedFile = LockedFile.getUnlocked(lockFile);
         try {
            lockedFile.lock();
            koronaRelayStatus = new KoronaRelayStatus(statusFile);
         } catch (Exception e) {
            Utils.closeOrSuppress(e, lockedFile);
            throw e;
         }
      }

      @Override
      public List<KoronaRelayUpdate> getUpdates() {
         return koronaRelayStatus.getAllCompletedFiles();
      }

      @Override
      public void saveStatus() throws IOException {
         koronaRelayStatus.save();
      }

      @Override
      public void close() throws IOException {
         try (lockedFile) {
            saveStatus();
         }
      }
   }
}
