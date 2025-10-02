package no.imr.tools.swing;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * A transfer handler accepting a list of files.
 */
public final class FileListTransferHandler extends TransferHandler {
   private final Consumer<List<Path>> fileHandler;

   public FileListTransferHandler(Consumer<List<Path>> fileHandler) {
      this.fileHandler = fileHandler;
   }

   @Override
   public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
      Set<DataFlavor> supportedFlavors = Set.of(DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor);
      return Arrays.stream(transferFlavors).anyMatch(supportedFlavors::contains);
   }

   @Override
   public boolean importData(JComponent comp, Transferable t) {
      try {
         if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            List<?> transferData = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
            List<Path> files = Utils.getAllOfType(transferData, File.class)
                  .map(File::toPath)
                  .toList();
            handleFiles(files);
            return true;
         }
         if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            String transferData = (String) t.getTransferData(DataFlavor.stringFlavor);
            List<Path> files = transferData.lines()
                  .map(s -> {
                     s = s.trim();
                     String prefix = "file:";
                     if (s.startsWith(prefix)) {
                        s = s.substring(prefix.length());
                     }
                     return s;
                  })
                  .filter(Predicate.not(String::isEmpty))
                  .map(Path::of)
                  .toList();
            handleFiles(files);
            return true;
         }
      } catch (UnsupportedFlavorException | IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      } catch (RuntimeException e) { // Avoids that the exception is swallowed silently in TransferHandler
         Log.global.log(Level.WARNING, e.getMessage(), e);
         throw e;
      }

      return false;
   }

   private void handleFiles(List<Path> files) {
      // Use invokeLater to avoid blocking (by modal dialogs) of the program where the files were dragged from.
      SwingUtilities.invokeLater(() -> fileHandler.accept(files));
   }
}
