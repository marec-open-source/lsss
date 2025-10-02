package no.imr.korona.data.formats.ek60.calibration;

import no.imr.korona.Korona;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.xml.XmlUtils;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CalibrationGui {
   private CalibrationGui() {
   }

   public static void edit(Path dir, JComponent referenceComponent) {
      GuiUtils.desktopEdit(dir.resolve(CalibrationFile.FILE_NAME), referenceComponent);
   }

   public static void createEmptyOrEdit(Path dir, JComponent referenceComponent) {
      Path calibrationFile = dir.resolve(CalibrationFile.FILE_NAME);
      if (Files.exists(calibrationFile)) {
         edit(dir, referenceComponent);
      } else {
         int answer = JOptionPane.showConfirmDialog(referenceComponent, "Calibration file does not exist.\n" + calibrationFile + "\nCreate?",
               "Create calibration file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
         if (answer != JOptionPane.OK_OPTION) {
            return;
         }
         createEmpty(dir, referenceComponent);
      }
   }

   public static void createEmpty(Path dir, JComponent referenceComponent) {
      Path file = dir.resolve(CalibrationFile.FILE_NAME);
      try {
         FileUtils.copy(Korona.getInstallationDataDir().resolve(CalibrationFile.FILE_NAME), file);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error creating " + file, e);
         return;
      }

      edit(dir, referenceComponent);
   }

   public static void generate(List<SegmentHandle> segmentHandles, JComponent referenceComponent) {
      if (segmentHandles.isEmpty()) {
         JOptionPane.showMessageDialog(referenceComponent, "No files.");
         return;
      }
      Path dir = segmentHandles.getFirst().getMainFile().getParent();
      ProgressView progressView = new ProgressView("Scanning files...", segmentHandles.size());
      CalibrationContent calibrationContent = new WorkerDialog(referenceComponent, progressView.getComponent())
            .setOnError(e -> GuiUtils.showErrorDialog(referenceComponent, "Error scanning data files in " + dir, e))
            .startMakeValue(asyncHandle -> {
               CalibrationFile calibrationFile = CalibrationFile.forDirectory(dir);
               return CalibrationGenerator.extend(calibrationFile.getContent(), segmentHandles, asyncHandle);
            });
      if (calibrationContent == null) {
         return;
      }

      Path file = dir.resolve(CalibrationFile.FILE_NAME);
      try {
         XmlUtils.writeDocument(calibrationContent.toXml(), file);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error saving " + file, e);
         return;
      }

      edit(dir, referenceComponent);
   }
}
