package no.imr.tools.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public record FileInfo(Path file, BasicFileAttributes attributes) {

   public FileInfo(Path file) throws IOException {
      this(file, Files.readAttributes(file, BasicFileAttributes.class));
   }

   @Override
   public String toString() {
      return file
            + ", created: " + attributes.creationTime().toInstant()
            + ", lastModified: " + attributes.lastModifiedTime().toInstant();
   }

   public String getFileName() {
      return file.getFileName().toString();
   }

   public boolean isDirectory() {
      return attributes.isDirectory();
   }

   public FileTime lastModifiedTime() {
      return attributes.lastModifiedTime();
   }
}
