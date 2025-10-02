package no.imr.lsss.framework.backup;

import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.DirectoryListing;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

final class RecursiveCopier {
   private final List<CopyJob> copyJobs = new ArrayList<>();
   private final AtomicLong numberOfFilesToCopy = new AtomicLong();

   RecursiveCopier() {
   }

   void scan(List<CopyItem> copyItems, Predicate<Path> fileFilter,
             AsyncHandle asyncHandle, Listener listener) {

      Deque<CopyItem> scanJobs = new ArrayDeque<>(copyItems);

      while (!asyncHandle.isCancelled()) {
         CopyItem scanJob = scanJobs.pollLast();
         if (scanJob == null) {
            break;
         }
         Path sourceDir = scanJob.sourceDir();
         List<FileInfo> sourceFileInfos;
         try {
            sourceFileInfos = FileUtils.listFilesWithAttributes(sourceDir, asyncHandle);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error accessing " + sourceDir, e);
            continue;
         }
         Path destDir = scanJob.destDir();
         DirectoryListing destDirListing;
         try {
            destDirListing = DirectoryListing.of(destDir, asyncHandle);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error accessing " + destDir, e);
            continue;
         }
         List<String> filesNames = new ArrayList<>();
         for (FileInfo sourceFileInfo : sourceFileInfos) {
            Path sourceFile = sourceFileInfo.file();
            if (!fileFilter.test(sourceFile)) {
               continue;
            }
            Path destFile = destDir.resolve(sourceFile.getFileName());
            if (sourceFileInfo.isDirectory()) {
               scanJobs.add(new CopyItem(sourceFile, destFile));
            } else {
               BasicFileAttributes destAttrs = destDirListing.map().get(destFile);
               if (destAttrs == null || destAttrs.lastModifiedTime().compareTo(sourceFileInfo.lastModifiedTime()) < 0) {
                  filesNames.add(sourceFile.getFileName().toString());
               }
            }
         }
         if (!filesNames.isEmpty()) {
            filesNames.sort(null);
            copyJobs.add(new CopyJob(sourceDir, destDir, filesNames));
            numberOfFilesToCopy.addAndGet(filesNames.size());
            listener.listen();
         }
      }
      copyJobs.sort(Comparator.comparing(CopyJob::sourceDir));
   }

   long getNumberOfFilesToCopy() {
      return numberOfFilesToCopy.get();
   }

   void copy(AsyncHandle asyncHandle, Consumer<Path> listener) {
      for (CopyJob copyJob : copyJobs) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         Path sourceDir = copyJob.sourceDir;
         Path destDir = copyJob.destDir;
         try {
            FileUtils.createDirectories(destDir);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error creating directory " + destDir + ": " + e);
            continue;
         }
         for (String fileName : copyJob.fileNames) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            Path sourceFile = sourceDir.resolve(fileName);
            Path destFile = destDir.resolve(fileName);
            listener.accept(sourceFile);
            try {
               FileUtils.copy(sourceFile, destFile);
            } catch (IOException e) {
               Log.global.log(Level.WARNING, "Error copying " + sourceFile + " to " + destFile + ": " + e);
            }
         }
      }
   }

   private record CopyJob(Path sourceDir, Path destDir, List<String> fileNames) {
   }
}
