package no.imr.tools.io;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

final class RecursiveEqualFileVisitor extends SimpleFileVisitor<Path> {
   private final Path pathA;
   private final Path pathB;
   private final Predicate<Path> fileFilter;
   private boolean result = true;

   RecursiveEqualFileVisitor(Path pathA, Path pathB, Predicate<Path> fileFilter) {
      this.pathA = pathA;
      this.pathB = pathB;
      this.fileFilter = fileFilter;
   }

   boolean getResult() {
      return result;
   }

   @Override
   public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      if (!fileFilter.test(dir)) {
         return FileVisitResult.SKIP_SUBTREE;
      }
      if (!Files.isDirectory(pathB.resolve(pathA.relativize(dir)))) {
         result = false;
         return FileVisitResult.TERMINATE;
      }
      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      if (!fileFilter.test(file)) {
         return FileVisitResult.CONTINUE;
      }
      if (!FileUtils.equals(file, pathB.resolve(pathA.relativize(file)))) {
         result = false;
         return FileVisitResult.TERMINATE;
      }
      return FileVisitResult.CONTINUE;
   }
}
