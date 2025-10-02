package no.imr.tools.io;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

/**
 * Obtains a lock on the input file.
 * When done with the lock {@link #close()} must be called.
 */
public final class LockedFile implements AutoCloseable {
   private static final LoadingCache<Path, LockedFile> LOCKED_FILES = CacheBuilder.newBuilder()
         .weakValues()
         .build(new CacheLoader<>() {
            @Override
            public LockedFile load(Path file) {
               return new LockedFile(file);
            }
         });

   private final Semaphore semaphore = new Semaphore(1);
   private final Path file;
   private @Nullable FileLock lock;

   private LockedFile(Path file) {
      this.file = file;
   }

   public static LockedFile getUnlocked(Path file) {
      return LOCKED_FILES.getUnchecked(file);
   }

   private void acquireSemaphore() {
      while (true) {
         try {
            semaphore.acquire();
            break;
         } catch (InterruptedException e) {
            // try again
         }
      }
   }

   public void lock() throws IOException {
      acquireSemaphore();
      FileChannel channel = FileUtils.openWritableChannel(file);
      lock = getLock(channel);
   }

   public void lockCreatingDirectories() throws IOException {
      acquireSemaphore();
      FileChannel channel = openChannelCreatingDirectories();
      lock = getLock(channel);
   }

   private FileChannel openChannelCreatingDirectories() throws IOException {
      try {
         return FileUtils.openWritableChannel(file);
      } catch (IOException e) {
         Path dir = file.getParent();
         if (dir == null || Files.exists(dir)) {
            throw e;
         }
         FileUtils.createDirectories(dir);
         return FileUtils.openWritableChannel(file);
      }
   }

   @Override
   public void close() throws IOException {
      try {
         if (lock != null) {
            lock.channel().close();
            lock = null;
         }
         // Lock file cannot be deleted safely.
         // See https://unix.stackexchange.com/questions/368159/why-flock-doesnt-clean-the-lock-file/368167#368167
      } finally {
         if (semaphore.availablePermits() == 0) {
            semaphore.release();
         }
      }
   }

   private static FileLock getLock(FileChannel channel) throws IOException {
      while (true) {
         try {
            return channel.lock();
         } catch (OverlappingFileLockException e) {
            Utils.sleep(1000);
         }
      }
   }
}
