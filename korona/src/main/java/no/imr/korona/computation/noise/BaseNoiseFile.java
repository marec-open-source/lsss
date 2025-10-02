package no.imr.korona.computation.noise;

import no.imr.tools.io.FileUtils;
import no.imr.tools.io.LockedFile;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public abstract class BaseNoiseFile {
   private final @Nullable Path file;

   protected BaseNoiseFile(@Nullable Path file) {
      this.file = file;
   }

   protected abstract Document mergedNoiseDocument(@Nullable Document documentOnFile);

   protected void writeNoiseFile() {
      if (file == null) {
         return;
      }
      try (LockedFile lockedFile = LockedFile.getUnlocked(getLockFile(file))) {
         lockedFile.lockCreatingDirectories();
         Document document = mergedNoiseDocument(XmlUtils.readDocumentIfExists(file));
         XmlUtils.writeDocument(document, file);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error saving noise file " + file, e);
      }
   }

   protected @Nullable Document readNoiseFile() throws IOException {
      if (file == null) {
         return null;
      }
      try (LockedFile lockedFile = LockedFile.getUnlocked(getLockFile(file))) {
         lockedFile.lock();
         return XmlUtils.readDocumentIfExists(file);
      } catch (IOException e) {
         if (!Files.exists(file.getParent())) {
            return null;
         }
         throw e;
      }
   }

   private static Path getLockFile(Path file) {
      return FileUtils.addSuffix(file, FileUtils.LOCK_FILE_SUFFIX);
   }
}
