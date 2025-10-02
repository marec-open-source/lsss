package no.imr.lsss.framework.backup;

import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.SwingDelayer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

final class CopyGui {
   private final List<CopyItem> copyItems;
   private final Set<Path> excludedSourceDirs;
   private final List<BackupExclusionOption> exclusionOptions;
   private final JComponent mainComponent;
   private final JLabel dirLabel = new JLabel("\u2007".repeat(60));
   private final JLabel fileLabel = new JLabel(" ");
   private final JProgressBar progressBar = new JProgressBar();

   CopyGui(List<CopyItem> copyItems,
           Set<Path> excludedSourceDirs, List<BackupExclusionOption> exclusionOptions) {
      this.copyItems = copyItems;
      this.excludedSourceDirs = excludedSourceDirs;
      this.exclusionOptions = exclusionOptions;

      progressBar.setStringPainted(true);
      progressBar.setString("Preparing to copy files...");
      progressBar.setMaximum(10_000);
      progressBar.setIndeterminate(true);
      GridBag gridBag = new GridBag()
            .configureVerticalBox()
            .activateVerticalFill();
      gridBag.add(new JLabel("Copying files..."));
      gridBag.add(dirLabel);
      gridBag.add(fileLabel);
      gridBag.add(progressBar);
      mainComponent = gridBag.getPanel();
   }

   JComponent getComponent() {
      return mainComponent;
   }

   void run(AsyncHandle asyncHandle) {
      Predicate<Path> fileFilter = file -> {
         return !excludedSourceDirs.contains(file)
               && !file.toString().endsWith(FileUtils.LOCK_FILE_SUFFIX);
      };
      for (BackupExclusionOption exclusionOption : exclusionOptions) {
         SwingUtilities.invokeLater(() -> progressBar.setString("Evaluating " + exclusionOption.name().displayName()));
         fileFilter = fileFilter.and(Predicate.not(exclusionOption.excludePredicateSupplier().get()));
      }
      RecursiveCopier recursiveCopier = new RecursiveCopier();
      recursiveCopier.scan(copyItems, fileFilter, asyncHandle, GuiListeners.coalescingLater(() -> {
         progressBar.setString("Scanning for files to copy... (" + recursiveCopier.getNumberOfFilesToCopy() + ")");
      }));
      long numberOfFilesToCopy = recursiveCopier.getNumberOfFilesToCopy();
      SwingUtilities.invokeLater(() -> {
         progressBar.setIndeterminate(false);
         progressBar.setString(0 + " / " + numberOfFilesToCopy);
      });
      AtomicLong counter = new AtomicLong();
      recursiveCopier.copy(asyncHandle, file -> {
         counter.incrementAndGet();
         SwingDelayer.invokeLater(counter, () -> {
            double fraction = counter.get() / (double) numberOfFilesToCopy;
            int value = (int) Math.floor(fraction * progressBar.getMaximum());
            progressBar.setValue(value);
            progressBar.setString(counter.get() + " / " + numberOfFilesToCopy);
            Path dir = file.getParent();
            dirLabel.setText(dir != null ? dir.toString() : file.toString());
            fileLabel.setText(dir != null ? file.getFileName().toString() : "");
         });
      });
   }
}
