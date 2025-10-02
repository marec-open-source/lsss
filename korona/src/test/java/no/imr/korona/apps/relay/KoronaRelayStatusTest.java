package no.imr.korona.apps.relay;

import no.imr.korona.data.formats.ek60.EK60DataFormatPlugin;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.test.UniqueTmpDir;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class KoronaRelayStatusTest {
   /**
    * Tests the functionality and interplay of {@link KoronaRelayStatus}, {@link KoronaRelayUpdateChecker} and
    * {@link KoronaRelayUpdate}.
    *
    * @throws IOException if something fails
    */
   @Test
   void test() throws IOException {
      //Create two KoronaRelay-like processed files.

      Path tmpDirFile = UniqueTmpDir.newSubDir("KoronaRelayStatusTest");
      FileUtils.createDirectories(tmpDirFile);

      Path raw1 = Files.createTempFile(tmpDirFile, "test", "-korona.raw.new");
      Path raw2 = Files.createTempFile(tmpDirFile, "test", "-korona.raw.new");

      String s1 = raw1.getFileName().toString().split("\\.")[0];
      String s2 = raw2.getFileName().toString().split("\\.")[0];

      FileUtils.copy(raw1, tmpDirFile.resolve(s1 + ".idx.new"));
      Path idx1 = tmpDirFile.resolve(s1 + ".idx.new");
      FileUtils.copy(raw2, tmpDirFile.resolve(s2 + ".idx.new"));
      Path idx2 = tmpDirFile.resolve(s2 + ".idx.new");

      FileUtils.copy(raw1, tmpDirFile.resolve(s1 + ".bot.new"));
      Path bot1 = tmpDirFile.resolve(s1 + ".bot.new");
      FileUtils.copy(raw2, tmpDirFile.resolve(s2 + ".bot.new"));
      Path bot2 = tmpDirFile.resolve(s2 + ".bot.new");

      String raw1BaseName = raw1.getFileName().toString().substring(0, raw1.getFileName().toString().indexOf(".raw.new"));
      String raw2BaseName = raw2.getFileName().toString().substring(0, raw2.getFileName().toString().indexOf(".raw.new"));

      //Get a KoronaRelayStatus object for this directory.
      Path file = tmpDirFile.resolve(KoronaRelay.STATUS_FILENAME);
      KoronaRelayStatus koronaRelayStatus = new KoronaRelayStatus(file);

      //KoronaRelay would add the newly processed files to the koronaRelayStatus and update it.
      koronaRelayStatus.getFilesReadyForCopy().add(raw1);
      koronaRelayStatus.getFilesReadyForCopy().add(idx1);
      koronaRelayStatus.getFilesReadyForCopy().add(bot1);

      koronaRelayStatus.getFilesReadyForCopy().add(raw2);
      koronaRelayStatus.getFilesReadyForCopy().add(idx2);
      koronaRelayStatus.getFilesReadyForCopy().add(bot2);

      koronaRelayStatus.save();
      //XmlUtils.writeDocument(koronaRelayStatus.toXml(), koronaRelayStatus.getStatusFile());

      //A client, for example LSSS, creates a KoronaRelayUpdateChecker object
      //and asks for pending updates.

      KoronaRelayUpdateChecker koronaRelayUpdateChecker = new KoronaRelayUpdateChecker(tmpDirFile);
      try (KoronaRelayUpdateChecker.UpdateLoader updateLoader = koronaRelayUpdateChecker.createUpdateLoader(true)) {
         assertEquals(6, updateLoader.getUpdates().size());

         //Loop over the updates and copy them. As they are copied, they will remove themselves from
         //the status file.
         for (KoronaRelayUpdate koronaRelayUpdate : updateLoader.getUpdates()) {
            koronaRelayUpdate.move();
         }
      }

      //Check that the .raw file now exists, and that the .new file is deleted.

      Path outputRaw1 = tmpDirFile.resolve(raw1BaseName + EK60DataFormatPlugin.RAW_SUFFIX);
      Path outputRaw2 = tmpDirFile.resolve(raw2BaseName + EK60DataFormatPlugin.RAW_SUFFIX);

      assertTrue(Files.exists(outputRaw1));
      assertTrue(Files.exists(outputRaw2));
      assertFalse(Files.exists(raw1));

      //Open the status file again. This should now be empty.
      KoronaRelayStatus koronaRelayStatus3 = new KoronaRelayStatus(file);
      assertEquals(0, koronaRelayStatus3.getAllCompletedFiles().size());

      try (KoronaRelayUpdateChecker.UpdateLoader updateLoader = koronaRelayUpdateChecker.createUpdateLoader(true)) {
         //Should get the same when asking for pending updates.
         assertEquals(0, updateLoader.getUpdates().size());
      }

      //Clean up
      FileUtils.deleteRecursively(tmpDirFile);
   }
}
