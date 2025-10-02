package no.imr.korona.apps.relay;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class KoronaRelayUtils {
   private KoronaRelayUtils() {
   }

   private static void recreateStatusXml(Path statusFile) throws IOException {
      Files.deleteIfExists(statusFile);
      KoronaRelayStatus koronaRelayStatus = new KoronaRelayStatus(statusFile);
      List<Path> newFiles = FileUtils.listFiles(statusFile.getParent(), FilePredicates.endsWith(KoronaRelay.NEW_SUFFIX));
      newFiles.sort(null);
      koronaRelayStatus.getFilesReadyForCopy().addAll(newFiles);
      koronaRelayStatus.save();
   }

   public static boolean showReCreateStatusXmlDialog(Component referenceComponent, Path statusFile) {
      int answer = GuiUtils.showOptionDialog(referenceComponent, "Re-create status.xml", "Do you wish to re-create the file\n" +
            statusFile, new String[]{"Re-create", "Cancel"});
      if (answer != 0) {
         return false;
      }
      return new WorkerDialog(referenceComponent, "Re-creating status.xml\n" + statusFile)
            .setOnError(e -> GuiUtils.showErrorDialog(referenceComponent, "Error re-creating " + statusFile, e))
            .startWithoutCancel(() -> {
               recreateStatusXml(statusFile);
            })
            .success();
   }

   public static void showStatusFileErrorDialog(@Nullable Component referenceComponent, Exception exception, Path statusFile) {
      HtmlStringBuilder message = new HtmlStringBuilder()
            .text(exception.getMessage());
      Throwable cause = exception.getCause();
      if (cause != null) {
         message.html("<br>").text(cause.toString());
      }
      int answer = GuiUtils.showOptionDialog(referenceComponent, "Error with status.xml", message.build(), new String[]{"Re-create status.xml", "Close"});
      if (answer == 0) {
         new WorkerDialog(referenceComponent, "Re-creating status.xml\n" + statusFile)
               .setOnError(e -> GuiUtils.showErrorDialog(referenceComponent, "Error re-creating " + statusFile, e))
               .startWithoutCancel(() -> recreateStatusXml(statusFile));
      }
   }

   public static String baseNameWithoutKoronaSuffix(SegmentHandle segmentHandle) {
      return removeKoronaSuffix(segmentHandle.getBaseName());
   }

   public static String removeKoronaSuffix(String baseName) {
      while (baseName.endsWith(KoronaRelay.KORONA_SUFFIX)) {
         baseName = baseName.substring(0, baseName.length() - KoronaRelay.KORONA_SUFFIX.length());
      }
      return baseName;
   }
}
