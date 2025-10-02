package no.imr.tools.misc.test;

import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.ThreadDump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A temporary directory that is unique to this VM and is deleted on exit.
 */
@SuppressWarnings("PMD.SystemPrintln")
public final class UniqueTmpDir {
   private static final Path DIR = create();
   private static final AtomicLong SUB_DIR_COUNTER = new AtomicLong();

   static {
      Exec.CACHED_THREAD_POOL.execute(UniqueTmpDir::deleteOldTmpDirs);
      Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(UniqueTmpDir::cleanUp));
   }

   private UniqueTmpDir() {
   }

   public static Path get() {
      return DIR;
   }

   public static Path newSubDir(String prefix) {
      return DIR.resolve(prefix + "-" + SUB_DIR_COUNTER.incrementAndGet());
   }

   private static Path create() {
      try {
         Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"), "marec");
         FileUtils.createDirectories(tmpDir);
         String prefix = "UniqueTmp." + Utils.createLocalDateTimeFormatter("yyyy-MM-dd_HH-mm-ss").format(Instant.now()) + ".";
         return Files.createTempDirectory(tmpDir, prefix);
      } catch (IOException e) {
         e.printStackTrace(System.err);
         throw new AssertionError(e);
      }
   }

   private static void cleanUp() {
      try {
         System.out.println("Deleting " + DIR);
         if (!Exec.shutDownAndWait()) {
            System.out.println("Could not shut down executors");
            ThreadDump.print(System.out);
            Runtime.getRuntime().halt(2);
         }
         FileUtils.repeatedlyTryDeleteRecursively(DIR);
      } catch (Throwable e) {
         e.printStackTrace(System.err);
         Runtime.getRuntime().halt(1);
      }
   }

   private static void deleteOldTmpDirs() {
      try {
         for (Path file : FileUtils.listFiles(DIR.getParent())) {
            if (!file.getFileName().toString().startsWith("UniqueTmp.")) {
               continue;
            }
            try {
               long time = FileUtils.lastModified(file);
               if (time > System.currentTimeMillis() - 24 * 3600 * 1000) {
                  continue;
               }
               FileUtils.deleteRecursively(file);
            } catch (IOException e) {
               e.printStackTrace(System.err);
            }
         }
      } catch (IOException e) {
         e.printStackTrace(System.err);
      }
   }
}
