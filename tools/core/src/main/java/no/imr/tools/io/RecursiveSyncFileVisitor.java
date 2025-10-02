package no.imr.tools.io;

import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

final class RecursiveSyncFileVisitor extends SimpleFileVisitor<Path> {
   private final AsyncHandle asyncHandle;
   private int level;
   private Path currentDestination;
   private final Deque<Map<Path, BasicFileAttributes>> parentDestinationMaps = new ArrayDeque<>();
   private Map<Path, BasicFileAttributes> currentDestinationMap = new HashMap<>();

   RecursiveSyncFileVisitor(Path destination, AsyncHandle asyncHandle) throws IOException {
      currentDestination = destination;
      this.asyncHandle = asyncHandle;

      BasicFileAttributes attributes = FileUtils.readAttributesIfExists(destination);
      if (attributes != null) {
         currentDestinationMap.put(destination, attributes);
      } else {
         FileUtils.createDirectories(destination.getParent());
      }
   }

   @Override
   public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (asyncHandle.isCancelled()) {
         return FileVisitResult.TERMINATE;
      }
      if (level > 0) {
         // This is a subdirectory.
         currentDestination = currentDestination.resolve(dir.getFileName());
      }
      level++;
      BasicFileAttributes destAttr = currentDestinationMap.remove(currentDestination);
      if (destAttr == null) {
         // It does not exist.
         FileUtils.createDirectories(currentDestination);
      } else if (!destAttr.isDirectory()) {
         // It exists, but is not a directory.
         Files.delete(currentDestination);
         FileUtils.createDirectories(currentDestination);
      }
      parentDestinationMaps.addLast(currentDestinationMap);
      currentDestinationMap = new HashMap<>();
      for (FileInfo fileInfo : FileUtils.listFilesWithAttributes(currentDestination, asyncHandle)) {
         currentDestinationMap.put(fileInfo.file(), fileInfo.attributes());
      }
      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
      level--;
      if (level > 0) {
         currentDestination = currentDestination.getParent();
      }
      for (Path toBeDeleted : currentDestinationMap.keySet()) {
         FileUtils.deleteRecursively(toBeDeleted);
      }
      currentDestinationMap = parentDestinationMaps.removeLast();
      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (asyncHandle.isCancelled()) {
         return FileVisitResult.TERMINATE;
      }
      Path dest = level == 0
            ? currentDestination // Copying only a single file.
            : currentDestination.resolve(file.getFileName());
      BasicFileAttributes destAttrs = currentDestinationMap.remove(dest);
      if (destAttrs != null && destAttrs.isDirectory()) {
         // It exists and is a directory.
         FileUtils.deleteRecursively(dest);
      }
      copy(file, attrs, dest, destAttrs);
      return FileVisitResult.CONTINUE;
   }

   private static void copy(Path src, BasicFileAttributes srcAttrs, Path dest, @Nullable BasicFileAttributes destAttrs) throws IOException {
      if (destAttrs == null
            || srcAttrs.size() != destAttrs.size()
            || !srcAttrs.lastModifiedTime().equals(destAttrs.lastModifiedTime())) {
         Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
      }
   }
}
