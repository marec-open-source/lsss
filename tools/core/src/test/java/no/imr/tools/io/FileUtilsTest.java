package no.imr.tools.io;

import no.imr.tools.misc.test.UniqueTmpDir;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

final class FileUtilsTest {
   @Test
   void baseName() {
      assertEquals("b", FileUtils.baseName(toPath("b")));
      assertEquals("b", FileUtils.baseName(toPath("b.c")));
      assertEquals("b.c", FileUtils.baseName(toPath("b.c.d")));
      assertEquals("b", FileUtils.baseName(toPath("a/b.c")));
      assertEquals("b.c", FileUtils.baseName(toPath("a/b.c.d")));
      assertEquals("b", FileUtils.baseName(toPath("d/a.x/b.c")));
   }

   @Test
   void relativePath() {
      assertEquals(toPath("a/b/c"), FileUtils.relativePath(toFile("a/b/c"), toFile(".")));
      assertEquals(toPath("../../.."), FileUtils.relativePath(toFile("."), toFile("a/b/c")));

      assertEquals(toPath("b/c"), FileUtils.relativePath(toFile("a/b/c"), toFile("a")));
      assertEquals(toPath("../.."), FileUtils.relativePath(toFile("a"), toFile("a/b/c")));

      assertEquals(toPath(""), FileUtils.relativePath(toFile("a/../c"), toFile("c")));
      assertEquals(toPath(""), FileUtils.relativePath(toFile("c"), toFile("a/../c")));

      assertEquals(toPath("../../c/d"), FileUtils.relativePath(toFile("a/b/c/d"), toFile("a/b/e/f")));
      assertEquals(toPath("../../e/f"), FileUtils.relativePath(toFile("a/b/e/f"), toFile("a/b/c/d")));

      Path root = FileUtils.listExistingRoots().getFirst();
      assertEquals(toPath(""), FileUtils.relativePath(root, root));

      assertEquals(toPath("a"), FileUtils.relativePath(root.resolve(toPath("a")), root));
      assertEquals(toPath(".."), FileUtils.relativePath(root, root.resolve(toPath("a"))));

      assertEquals(toPath("b"), FileUtils.relativePath(root.resolve(toPath("a/b")), root.resolve(toPath("a"))));
      assertEquals(toPath(".."), FileUtils.relativePath(root.resolve(toPath("a")), root.resolve(toPath("a/b"))));
   }

   @Test
   void inDir() {
      Path dir = Path.of("a", "b");
      assertTrue(FileUtils.isInDir(dir.resolve("c"), dir));
      assertTrue(FileUtils.isInDir(dir, dir));
      assertFalse(FileUtils.isInDir(dir.getParent(), dir));
   }

   @Test
   void isEmptyDirectory() throws IOException {
      Path dir = UniqueTmpDir.newSubDir("FileUtilsTest.isEmptyDirectory");
      FileUtils.createDirectories(dir);

      Path file = dir.resolve("file.txt");

      assertTrue(FileUtils.isEmptyDirectory(dir));
      assertFalse(FileUtils.isEmptyDirectory(file));

      Files.createFile(file);

      assertFalse(FileUtils.isEmptyDirectory(dir));
      assertFalse(FileUtils.isEmptyDirectory(file));

      FileUtils.deleteRecursively(dir);
   }

   @Test
   void listFiles() throws IOException {
      Path dir = UniqueTmpDir.newSubDir("FileUtilsTest.listFiles");
      FileUtils.createDirectories(dir);
      Path file = dir.resolve("file.txt");

      assertEquals(0, FileUtils.listFiles(dir).size());
      assertEquals(0, FileUtils.listFilesWithAttributes(dir).size());

      assertEquals(0, FileUtils.listFiles(file).size());
      assertEquals(0, FileUtils.listFilesWithAttributes(file).size());

      Files.createFile(file);

      assertEquals(1, FileUtils.listFiles(dir).size());
      assertEquals(1, FileUtils.listFilesWithAttributes(dir).size());

      assertEquals(0, FileUtils.listFiles(file).size());
      assertEquals(0, FileUtils.listFilesWithAttributes(file).size());

      FileUtils.deleteRecursively(dir);
   }

   @Test
   void writeNewFile() throws IOException {
      Path dir = UniqueTmpDir.newSubDir("FileUtilsTest.writeNewFile");
      byte[] contents = {1, 2, 3};

      Path newFile = FileUtils.writeNewFile(dir, "a-", ".dat", contents);
      assertArrayEquals(contents, Files.readAllBytes(newFile));

      newFile = FileUtils.writeNewFile(dir.resolve("nonExistingSubDir"), "a-", ".dat", contents);
      assertArrayEquals(contents, Files.readAllBytes(newFile));

      Path file = dir.resolve("file");
      Files.createFile(file);
      assertThrows(IOException.class, () -> {
         FileUtils.writeNewFile(file, "a-", ".dat", contents);
      });

      FileUtils.deleteRecursively(dir);
   }

   @Test
   void unzip() throws IOException {
      Path dir = UniqueTmpDir.newSubDir("FileUtilsTest.unzip");
      byte[] contents = {1, 2, 3};

      FileUtils.unzip(toZipInputStream("a/b.dat", contents), dir);
      assertArrayEquals(contents, Files.readAllBytes(dir.resolve(toPath("a/b.dat"))));

      assertThrows(IOException.class, () -> {
         FileUtils.unzip(toZipInputStream("../evil.dat", contents), dir);
      });
      assertThrows(IOException.class, () -> {
         FileUtils.unzip(toZipInputStream("a/../../evil.dat", contents), dir);
      });

      FileUtils.deleteRecursively(dir);
   }

   private static ZipInputStream toZipInputStream(String entryName, byte[] contents) throws IOException {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
         zipOutputStream.putNextEntry(new ZipEntry(entryName));
         zipOutputStream.write(contents);
         zipOutputStream.closeEntry();
      }
      return new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
   }

   private static Path toFile(String path) {
      return Path.of(toPath(path));
   }

   private static String toPath(String path) {
      return path.replace('/', File.separatorChar);
   }
}
