package no.imr.tools.io;

import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.ThrowingRunnable;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utilities for file handling.
 */
public final class FileUtils {
   public static final String GZIP_SUFFIX = ".gz";
   public static final String SCRIPT_SUFFIX = File.separatorChar == '/' ? ".sh" : ".bat";
   public static final String LOCK_FILE_SUFFIX = ".lck";

   private FileUtils() {
   }

   public static Path resolve(@Nullable Path dir, String other) {
      return dir != null ? dir.resolve(other) : Path.of(other);
   }

   /**
    * Returns the base name for a file.
    *
    * @param file a file
    * @return the first part of the file name up to, but not including the last '.', or the entire file name if it contains no '.'
    */
   public static String baseName(Path file) {
      return baseName(file.toString());
   }

   public static String baseName(String path) {
      int iSep = path.lastIndexOf(File.separatorChar);
      int iDot = path.lastIndexOf('.');
      return path.substring(iSep < 0 ? 0 : iSep + 1, iDot > iSep ? iDot : path.length());
   }

   /**
    * Returns the suffix for a file.
    *
    * @param file a file
    * @return the last part of the file name from and including the '.', or the empty string if it contains no suffix
    */
   public static String getSuffix(Path file) {
      return getSuffix(file.getFileName().toString());
   }

   public static String getSuffix(String name) {
      int i = name.lastIndexOf('.');
      return i == -1 ? "" : name.substring(i);
   }

   public static Path addSuffix(Path file, String suffix) {
      return file.resolveSibling(file.getFileName() + suffix);
   }

   public static Path ensureSuffix(Path file, String suffix) {
      if (file.toString().endsWith(suffix)) {
         return file;
      } else {
         return addSuffix(file, suffix);
      }
   }

   public static boolean isGzip(Path file) {
      return file.toString().endsWith(GZIP_SUFFIX);
   }

   public static Path addGzipSuffix(Path file) {
      return addSuffix(file, GZIP_SUFFIX);
   }

   public static @Nullable BasicFileAttributes readAttributesIfExists(Path file) throws IOException {
      try {
         return Files.readAttributes(file, BasicFileAttributes.class);
      } catch (IOException e) {
         if (notExists(e, file)) {
            return null;
         }
         throw e;
      }
   }

   public static boolean notExists(Throwable e, Path file) {
      // Cannot also test for FileNotFoundException since it may be thrown if file is inaccessible.
      return e instanceof NoSuchFileException
            || Files.notExists(file);
   }

   public static long sizeOr0(Path file) {
      try {
         return Files.size(file);
      } catch (IOException _) {
         return 0;
      }
   }

   public static long creationTime(Path file) throws IOException {
      return Files.readAttributes(file, BasicFileAttributes.class).creationTime().toMillis();
   }

   public static long lastModified(Path file) throws IOException {
      return Files.readAttributes(file, BasicFileAttributes.class).lastModifiedTime().toMillis();
   }

   public static long lastModifiedOr0(Path file) {
      try {
         return lastModified(file);
      } catch (IOException _) {
         return 0;
      }
   }

   public static long lastAccessed(Path file) throws IOException {
      return Files.readAttributes(file, BasicFileAttributes.class).lastAccessTime().toMillis();
   }

   public static void setLastAccessed(Path file, long lastAccessed) throws IOException {
      Files.setAttribute(file, "lastAccessTime", FileTime.fromMillis(lastAccessed));
   }

   private static LastModifiedAndSize getRecursiveLastModifiedAndSizeOr0(Collection<FileInfo> fileInfos, AsyncHandle asyncHandle) {
      return fileInfos.stream()
            .map(fileInfo -> getRecursiveLastModifiedAndSizeOr0(fileInfo, asyncHandle))
            .reduce(LastModifiedAndSize.ZERO, LastModifiedAndSize::combine);
   }

   public static LastModifiedAndSize getRecursiveLastModifiedAndSizeOr0(FileInfo fileInfo, AsyncHandle asyncHandle) {
      try {
         BasicFileAttributes attributes = fileInfo.attributes();
         if (attributes.isDirectory()) {
            List<FileInfo> fileInfos = listFilesWithAttributes(fileInfo.file(), asyncHandle);
            return getRecursiveLastModifiedAndSizeOr0(fileInfos, asyncHandle);
         } else {
            return new LastModifiedAndSize(attributes);
         }
      } catch (IOException _) {
         return LastModifiedAndSize.ZERO;
      }
   }

   public static boolean equals(Path file, byte[] bytes) {
      try {
         return Arrays.equals(Files.readAllBytes(file), bytes);
      } catch (IOException _) {
         return false;
      }
   }

   public static boolean equals(Path fileA, Path fileB) {
      try {
         return Arrays.equals(Files.readAllBytes(fileA), Files.readAllBytes(fileB));
      } catch (IOException _) {
         return false;
      }
   }

   public static boolean equalsRecursively(Path fileA, Path fileB) {
      return equalsRecursively(fileA, fileB, FilePredicates.includeAll());
   }

   public static boolean equalsRecursively(Path fileA, Path fileB, Predicate<Path> fileFilter) {
      try {
         RecursiveEqualFileVisitor visitor = new RecursiveEqualFileVisitor(fileA, fileB, fileFilter);
         Files.walkFileTree(fileA, visitor);
         return visitor.getResult();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
         return false;
      }
   }

   public static void transfer(InputStream in, Path file) throws IOException {
      Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
   }

   public static void copy(URL sourceUrl, Path destinationFile) throws IOException {
      try (InputStream in = sourceUrl.openStream()) {
         transfer(in, destinationFile);
      }
   }

   public static void copy(Path sourceFile, Path destinationFile) throws IOException {
      Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
   }

   public static InputStream newBufferedInputStream(Path file) throws IOException {
      return new BufferedInputStream(Files.newInputStream(file));
   }

   public static BufferedReader newBufferedReader(URL url, Charset charset) throws IOException {
      return new BufferedReader(new InputStreamReader(url.openStream(), charset));
   }

   public static PrintWriter newPrintWriter(Path file, Charset charset) throws IOException {
      return new PrintWriter(Files.newBufferedWriter(file, charset));
   }

   public static FileChannel openWritableChannel(Path file) throws IOException {
      return FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
   }

   public static byte[] readAllBytes(URL url) throws IOException {
      try (InputStream in = url.openStream()) {
         return in.readAllBytes();
      }
   }

   public static String readAsString(URL url, Charset charset) throws IOException {
      try (InputStream in = url.openStream()) {
         return readAsString(in, charset);
      }
   }

   public static String readAsString(InputStream in, Charset charset) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      in.transferTo(out);
      return out.toString(charset);
   }

   public static void write(WritableByteChannel channel, ByteBuffer byteBuffer) throws IOException {
      while (byteBuffer.hasRemaining()) {
         channel.write(byteBuffer);
      }
   }

   /**
    * Finds the deepest existing parent directory of a file.
    * If the file (or its parent directory) exists, this is the same as {@link Path#getParent()}.
    *
    * @param file a file
    * @return the deepest existing parent directory, or {@code null} if not available
    */
   public static @Nullable Path getExistingParent(Path file) {
      file = file.getParent();
      while (file != null && !Files.exists(file)) {
         file = file.getParent();
      }
      return file;
   }

   /**
    * Return parent directory or {@code null} of {@code file} is {@code null}.
    *
    * @param file a file
    * @return the parent directory or {@code null}
    */
   public static @Nullable Path getParent(@Nullable Path file) {
      return file != null ? file.getParent() : null;
   }

   /**
    * Deletes a file or directory recursively.
    * All contents of a directory is deleted.
    * Does not complain if the file does not exist.
    *
    * @param file a file or directory
    * @throws IOException if some IO error occurs
    */
   public static void deleteRecursively(Path file) throws IOException {
      if (!Files.exists(file)) {
         return;
      }
      Files.walkFileTree(file, new RecursiveDeleteFileVisitor());
   }

   public static void deleteContentsRecursively(Path dir) throws IOException {
      for (Path file : listFiles(dir)) {
         deleteRecursively(file);
      }
   }

   /**
    * Returns the relative path of a file relative to a reference directory.
    *
    * @param file               a file
    * @param referenceDirectory a directory
    * @return the relative path, or null if a relative path cannot be found
    */
   public static @Nullable String relativePath(Path file, Path referenceDirectory) {
      try {
         Path path = file.toAbsolutePath().normalize();
         Path referencePath = referenceDirectory.toAbsolutePath().normalize();
         return referencePath.relativize(path).toString();
      } catch (IllegalArgumentException _) {
         return null;
      }
   }

   /**
    * Tests if a file is in a directory.
    *
    * @param file a file
    * @param dir  a directory
    * @return true if file is in directory
    */
   public static boolean isInDir(Path file, Path dir) {
      file = file.toAbsolutePath().normalize();
      dir = dir.toAbsolutePath().normalize();
      return file.startsWith(dir);
   }

   /**
    * Modifies a path by replacing all occurrences of / and \ with {@link File#separatorChar}.
    *
    * @param path a path
    * @return the modified path
    */
   public static String toNativeSeparatorChar(String path) {
      return path.replace(File.separatorChar == '/' ? '\\' : '/', File.separatorChar);
   }

   public static String toSlashSeparatorChar(String path) {
      return path.replace(File.separatorChar, '/');
   }

   /**
    * Copies all files and directories recursively.
    *
    * @param source      file/directory to copy from. If directory, only the contents of the directory is copied, not
    *                    the directory itself
    * @param destination file/directory to copy to
    * @throws IOException if some IO error occurs
    */
   public static void copyRecursively(Path source, Path destination) throws IOException {
      copyRecursively(source, destination, Utils.emptyConsumer(), new AsyncHandle());
   }

   public static void copyRecursively(Path source, Path destination, Consumer<? super Path> listener, AsyncHandle asyncHandle) throws IOException {
      copyRecursively(source, destination, listener, asyncHandle, FilePredicates.includeAll());
   }

   public static void copyRecursively(Path source, Path destination, Consumer<? super Path> listener, AsyncHandle asyncHandle, Predicate<Path> fileFilter) throws IOException {
      Files.walkFileTree(source, new RecursiveCopyFileVisitor(destination, fileFilter, listener, asyncHandle));
   }

   public static void copyAllRecursively(Map<Path, Path> files, ProgressHandler progressHandler, AsyncHandle asyncHandle) {
      copyAllRecursively(files, progressHandler, asyncHandle, FilePredicates.includeAll());
   }

   public static void copyAllRecursively(Map<Path, Path> files, ProgressHandler progressHandler, AsyncHandle asyncHandle, Predicate<Path> fileFilter) {
      long totalCount = files.keySet().stream()
            .mapToLong(file -> {
               try {
                  return getRecursiveFileCount(file, fileFilter, Utils.emptyConsumer(), asyncHandle);
               } catch (IOException e) {
                  Log.global.log(Level.WARNING, e.getMessage(), e);
                  return 0;
               }
            })
            .sum();

      Listener progressListener = progressHandler.asCountingListener(totalCount);

      for (Map.Entry<Path, Path> entry : files.entrySet()) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         Path source = entry.getKey();
         Path destination = entry.getValue();
         try {
            deleteRecursively(destination);
            copyRecursively(source, destination, progressListener, asyncHandle, fileFilter);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error copying " + source + " to " + destination, e);
         }
      }
   }

   public static void syncRecursively(Path source, Path destination, AsyncHandle asyncHandle) throws IOException {
      RecursiveSyncFileVisitor visitor = new RecursiveSyncFileVisitor(destination, asyncHandle);
      if (Files.exists(source)) {
         Files.walkFileTree(source, visitor);
      } else {
         deleteRecursively(destination);
      }
   }

   public static long getRecursiveFileCount(Path file, Predicate<Path> fileFilter, Consumer<Long> listener, AsyncHandle asyncHandle) throws IOException {
      RecursiveCountFileVisitor visitor = new RecursiveCountFileVisitor(fileFilter, listener, asyncHandle);
      Files.walkFileTree(file, visitor);
      return visitor.getCount();
   }

   /**
    * Same as {@link Files#createDirectories(Path, FileAttribute[])}, but does not
    * throw an exception for a symbolic link pointing to a directory.
    *
    * @param dir the directory to create
    * @throws IOException if an I/O error occurs
    */
   public static void createDirectories(Path dir) throws IOException {
      try {
         Files.createDirectories(dir);
      } catch (IOException e) {
         try {
            if (Files.isDirectory(dir.toRealPath())) {
               // It is a symbolic link to a directory (#887)
               return;
            }
         } catch (IOException _) {
            // Ignore this exception and instead throw the first exception.
         }
         throw e;
      }
   }

   /**
    * Deletes a directory if it is empty.
    *
    * @param dir the directory to delete if empty
    * @throws IOException if the directory is empty and could not be deleted
    */
   public static void deleteDirectoryIfEmpty(Path dir) throws IOException {
      if (isEmptyDirectory(dir)) {
         try {
            Files.deleteIfExists(dir);
         } catch (DirectoryNotEmptyException _) {
            // Maybe a file was created since calling isEmptyDirectory.
         }
      }
   }

   public static boolean isEmptyDirectory(Path dir) throws IOException {
      if (!Files.isDirectory(dir)) {
         return false;
      }
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
         return !stream.iterator().hasNext();
      }
   }

   public static void move(Path from, Path to) throws IOException {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
   }

   public static List<Path> listFiles(Path dir) throws IOException {
      return listFiles(dir, new AsyncHandle());
   }

   public static List<Path> listFiles(Path dir, Predicate<Path> predicate) throws IOException {
      return listFiles(dir, new AsyncHandle(), predicate);
   }

   public static List<Path> listFiles(Path dir, AsyncHandle asyncHandle) throws IOException {
      return listFiles(dir, asyncHandle, FilePredicates.includeAll());
   }

   public static List<Path> listFiles(Path dir, AsyncHandle asyncHandle, Predicate<Path> predicate) throws IOException {
      List<Path> files = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
         for (Path file : stream) {
            if (asyncHandle.isCancelled()) {
               files.clear();
               break;
            }
            if (predicate.test(file)) {
               files.add(file);
            }
         }
      } catch (NotDirectoryException _) {
         // Not directory => return empty list.
      } catch (IOException e) {
         if (Files.isDirectory(dir)) {
            throw e;
         }
         // Not directory => return empty list.
      }
      return files;
   }

   public static List<FileInfo> listFilesWithAttributes(Path dir) throws IOException {
      return listFilesWithAttributes(dir, new AsyncHandle());
   }

   public static List<FileInfo> listFilesWithAttributes(Path dir, Predicate<FileInfo> predicate) throws IOException {
      return listFilesWithAttributes(dir, new AsyncHandle(), predicate);
   }

   public static List<FileInfo> listFilesWithAttributes(Path dir, AsyncHandle asyncHandle) throws IOException {
      return listFilesWithAttributes(dir, asyncHandle, _ -> true);
   }

   public static List<FileInfo> listFilesWithAttributes(Path dir, AsyncHandle asyncHandle, Predicate<FileInfo> predicate) throws IOException {
      List<FileInfo> fileInfos = new ArrayList<>();
      try {
         Files.walkFileTree(dir, Set.of(), 1, new SimpleFileVisitor<>() {
            private boolean inDirectory;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
               inDirectory = true;
               return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
               if (asyncHandle.isCancelled()) {
                  fileInfos.clear();
                  return FileVisitResult.TERMINATE;
               }
               if (inDirectory) {
                  FileInfo fileInfo = new FileInfo(file, attrs);
                  if (predicate.test(fileInfo)) {
                     fileInfos.add(fileInfo);
                  }
               }
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (IOException e) {
         if (Files.isDirectory(dir)) {
            throw e;
         }
         // Not directory => return empty list.
      }
      return fileInfos;
   }

   public static Map<Path, BasicFileAttributes> fileInfosToMap(List<FileInfo> fileInfos) {
      Map<Path, BasicFileAttributes> map = HashMap.newHashMap(fileInfos.size());
      fileInfos.forEach(fileInfo -> map.put(fileInfo.file(), fileInfo.attributes()));
      return map;
   }

   public static @Nullable File toFile(@Nullable Path path) {
      return path != null ? path.toFile() : null;
   }

   public static List<File> toFiles(Collection<Path> paths) {
      return paths.stream()
            .map(Path::toFile)
            .toList();
   }

   public static @Nullable Path toPath(@Nullable File file) {
      try {
         return file != null ? file.toPath() : null;
      } catch (InvalidPathException _) {
         // Happens on Windows when going up one level from list of drives.
         return null;
      }
   }

   public static List<Path> toPaths(Collection<File> files) {
      return files.stream()
            .map(File::toPath)
            .toList();
   }

   public static List<Path> listExistingRoots() {
      return StreamSupport.stream(FileSystems.getDefault().getRootDirectories().spliterator(), false)
            .filter(Files::exists)
            .sorted()
            .toList();
   }

   public static int read(ReadableByteChannel channel, ByteBuffer byteBuffer) throws IOException {
      int totalBytesRead = 0;
      while (byteBuffer.hasRemaining()) {
         int bytesRead = channel.read(byteBuffer);
         if (bytesRead < 0) {
            break;
         }
         totalBytesRead += bytesRead;
      }
      return totalBytesRead;
   }

   public static int read(FileChannel fileChannel, ByteBuffer byteBuffer, long filePosition) throws IOException {
      int totalBytesRead = 0;
      while (byteBuffer.hasRemaining()) {
         int bytesRead = fileChannel.read(byteBuffer, filePosition + totalBytesRead);
         if (bytesRead < 0) {
            break;
         }
         totalBytesRead += bytesRead;
      }
      return totalBytesRead;
   }

   public static ByteBuffer toByteBuffer(Path file, ByteOrder byteOrder) throws IOException {
      byte[] bytes = Files.readAllBytes(file);
      return ByteBuffer.wrap(bytes)
            .order(byteOrder);
   }

   public static boolean replaceFileSafely(Path file, String text, Charset charset) throws IOException {
      return replaceFileSafely(file, text.getBytes(charset));
   }

   public static boolean replaceFileSafely(Path file, byte[] bytes) throws IOException {
      if (equals(file, bytes)) {
         return false;
      }
      Path dir = file.getParent();
      Path newFile = writeNewFile(dir, file.getFileName().toString() + "-new-", "", bytes);
      try {
         move(newFile, file);
         return true;
      } catch (IOException e) {
         // Move failed, so delete temporary new file:
         try {
            Files.deleteIfExists(newFile);
         } catch (IOException suppressed) {
            e.addSuppressed(suppressed);
         }
         // Multiple threads concurrently writing to the same file can cause various IO exceptions,
         // such as AccessDeniedException, FileAlreadyExistsException, NoSuchFileException.
         // Wait a little and check if the file content matches:
         Utils.sleep(100);
         if (equals(file, bytes)) {
            return false;
         }
         throw e;
      }
   }

   public static Path writeNewFile(Path dir, String fileNamePrefix, String fileNameSuffix, byte[] content) throws IOException {
      try {
         // Try first optimistically assuming the directory exists and that we can guess an unused file name.
         String fileName = fileNamePrefix + Long.toHexString(Double.doubleToRawLongBits(Math.random())) + fileNameSuffix;
         Path file = dir.resolve(fileName);
         Files.write(file, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
         return file;
      } catch (IOException _) {
         // If failure, then do it safer (and slower) using more IO operations.
         createDirectories(dir);
         Path file = Files.createTempFile(dir, fileNamePrefix, fileNameSuffix);
         Files.write(file, content);
         return file;
      }
   }

   public static void repeatedlyTryDeleteRecursively(Path file) throws IOException {
      repeatedlyTry(() -> deleteRecursively(file));
   }

   /**
    * Repeatedly tries to do some io operation.
    * This is sometimes necessary on Windows.
    *
    * @param job the task to do
    * @throws IOException if not successful
    */
   public static void repeatedlyTry(ThrowingRunnable<IOException> job) throws IOException {
      int failureCount = 0;
      while (true) {
         try {
            job.run();
            // OK, return
            return;
         } catch (IOException e) {
            failureCount++;
            if (failureCount == 5) {
               // Too many tries, give up
               throw e;
            }
            // Wait a little and try again
            Utils.sleep(1000);
         }
      }
   }

   public static void zip(String pathPrefix, ZipOutputStream zipOutputStream, Path dir, AsyncHandle asyncHandle, Predicate<FileInfo> predicate) throws IOException {
      List<FileInfo> files = listFilesWithAttributes(dir, asyncHandle, predicate);
      files.sort(Comparator.comparing(FileInfo::isDirectory).thenComparing(FileInfo::file));
      for (FileInfo fileInfo : files) {
         Path file = fileInfo.file();
         if (fileInfo.isDirectory()) {
            String path = pathPrefix + file.getFileName() + "/";
            zipOutputStream.putNextEntry(new ZipEntry(path));
            zipOutputStream.closeEntry();
            zip(path, zipOutputStream, file, asyncHandle, predicate);
         } else {
            String path = pathPrefix + file.getFileName();
            zipOutputStream.putNextEntry(new ZipEntry(path));
            Files.copy(file, zipOutputStream);
            zipOutputStream.closeEntry();
         }
      }
   }

   public static void unzip(ZipInputStream zipInputStream, Path dir) throws IOException {
      while (true) {
         ZipEntry entry = zipInputStream.getNextEntry();
         if (entry == null) {
            break;
         }
         Path file = dir.resolve(toNativeSeparatorChar(entry.getName()));
         if (entry.isDirectory()) {
            createDirectories(file);
         } else {
            createDirectories(file.getParent());
            transfer(zipInputStream, file);
         }
         zipInputStream.closeEntry();
      }
   }
}
