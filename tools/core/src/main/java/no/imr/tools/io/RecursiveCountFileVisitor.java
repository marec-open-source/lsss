package no.imr.tools.io;

import no.imr.tools.concurrent.AsyncHandle;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class RecursiveCountFileVisitor extends SimpleFileVisitor<Path> {
   private final AsyncHandle asyncHandle;
   private final Consumer<Long> listener;
   private final Predicate<Path> fileFilter;
   private long count;

   RecursiveCountFileVisitor(Predicate<Path> fileFilter, Consumer<Long> listener, AsyncHandle asyncHandle) {
      this.fileFilter = fileFilter;
      this.listener = listener;
      this.asyncHandle = asyncHandle;
   }

   long getCount() {
      return count;
   }

   @Override
   public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      if (asyncHandle.isCancelled()) {
         return FileVisitResult.TERMINATE;
      }
      if (!fileFilter.test(dir)) {
         return FileVisitResult.SKIP_SUBTREE;
      }
      count++;
      listener.accept(count);
      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      if (asyncHandle.isCancelled()) {
         return FileVisitResult.TERMINATE;
      }
      if (fileFilter.test(file)) {
         count++;
         listener.accept(count);
      }
      return FileVisitResult.CONTINUE;
   }
}
