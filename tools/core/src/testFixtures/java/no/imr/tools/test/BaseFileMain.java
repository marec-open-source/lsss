package no.imr.tools.test;

import no.imr.tools.ToolsPreferences;
import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.FileListTransferHandler;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

/**
 * Base class for test applications that select a file and do something.
 */
public abstract class BaseFileMain {
   protected BaseFileMain() {
   }

   protected abstract void execute(Path file) throws IOException;

   protected void customizeFileChooser(JFileChooser fileChooser) {
   }

   private @Nullable Path selectFile() {
      Preferences preferences = ToolsPreferences.node("test").node(getClass().getSimpleName());
      String fileKey = "file";
      String path = preferences.get(fileKey, null);
      JFileChooser fileChooser = new JFileChooser();
      if (path != null) {
         Path file = Path.of(path);
         if (Files.exists(file)) {
            fileChooser.setSelectedFile(file.toFile());
         } else {
            fileChooser.setCurrentDirectory(FileUtils.toFile(FileUtils.getExistingParent(file)));
         }
      }
      fileChooser.setTransferHandler(new FileListTransferHandler(files -> fileChooser.setSelectedFile(files.getFirst().toFile())));
      customizeFileChooser(fileChooser);
      int state = fileChooser.showOpenDialog(null);
      if (state == JFileChooser.APPROVE_OPTION) {
         Path selectedFile = fileChooser.getSelectedFile().toPath();
         preferences.put(fileKey, selectedFile.toString());
         return selectedFile;
      } else {
         return null;
      }
   }

   public void start() {
      SwingUtilities.invokeLater(() -> {
         try {
            Path file = selectFile();
            if (file != null) {
               execute(file);
            }
         } catch (IOException e) {
            throw new UncheckedIOException(e);
         }
      });
   }
}
