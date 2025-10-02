package no.imr.tools.io;

import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class RecursiveCopyFileVisitor extends SimpleFileVisitor<Path> {
   private final Predicate<Path> fileFilter;
   private final Consumer<? super Path> listener;
   private final AsyncHandle asyncHandle;
   private int level;
   private Path currentDestinationDir;

   RecursiveCopyFileVisitor(Path destination, Predicate<Path> fileFilter, Consumer<? super Path> listener, AsyncHandle asyncHandle) throws IOException {
      this.fileFilter = fileFilter;
      this.listener = listener;
      this.asyncHandle = asyncHandle;
      currentDestinationDir = destination;

      FileUtils.createDirectories(destination.getParent());
   }

   @Override
   public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (asyncHandle.isCancelled()) {
         return FileVisitResult.TERMINATE;
      }
      if (!fileFilter.test(dir)) {
         return FileVisitResult.SKIP_SUBTREE;
      }
      listener.accept(dir);
      if (level > 0) {
         currentDestinationDir = currentDestinationDir.resolve(dir.getFileName().toString());
      }
      level++;
      FileUtils.createDirectories(currentDestinationDir);
      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) {
      level--;
      if (level > 0) {
         currentDestinationDir = currentDestinationDir.getParent();
      }
      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (asyncHandle.isCancelled()) {
         return FileVisitResult.TERMINATE;
      }
      if (fileFilter.test(file)) {
         listener.accept(file);
         Path destination;
         if (level > 0 || Files.isDirectory(currentDestinationDir)) {
            destination = currentDestinationDir.resolve(file.getFileName().toString());
         } else {
            destination = currentDestinationDir;
         }
         FileUtils.copy(file, destination);
      }
      return FileVisitResult.CONTINUE;
   }
}
