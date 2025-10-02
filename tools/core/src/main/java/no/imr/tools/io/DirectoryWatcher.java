package no.imr.tools.io;

import no.imr.tools.concurrent.Exec;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class DirectoryWatcher {
   private final Consumer<Set<Path>> listener;
   private final WatchService watchService;
   private final WatchKey watchKey;
   private final ScheduledFuture<?> future;

   public DirectoryWatcher(Path dir, Consumer<Set<Path>> listener) throws IOException {
      this.listener = listener;
      watchService = dir.getFileSystem().newWatchService();
      watchKey = dir.register(watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY);
      future = Exec.scheduleWithFixedDelay(this::check, 1000, TimeUnit.MILLISECONDS);
   }

   public static @Nullable DirectoryWatcher forDir(Path dir, Consumer<Set<Path>> listener) {
      try {
         return new DirectoryWatcher(dir, listener);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error watching " + dir, e);
         return null;
      }
   }

   public static @Nullable DirectoryWatcher forFile(Path file, Runnable listener) {
      return forDir(file.getParent(), files -> {
         if (files.contains(file)) {
            listener.run();
         }
      });
   }

   private void check() {
      Set<Path> changedFiles = null;
      while (true) {
         WatchKey key;
         try {
            key = watchService.poll();
         } catch (ClosedWatchServiceException e) {
            return;
         }
         if (key == null) {
            break;
         }
         Path dir = (Path) key.watchable();
         for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();
            if (kind == StandardWatchEventKinds.OVERFLOW) {
               continue;
            }
            Path context = (Path) event.context();
            Path file = dir.resolve(context);
            if (changedFiles == null) {
               changedFiles = new HashSet<>();
            }
            changedFiles.add(file);
         }
         key.reset();
      }
      if (changedFiles != null) {
         listener.accept(changedFiles);
      }
   }

   public void close() {
      future.cancel(true);
      watchKey.cancel();
      try {
         watchService.close();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error stopping watch service for " + watchKey.watchable(), e);
      }
   }
}
