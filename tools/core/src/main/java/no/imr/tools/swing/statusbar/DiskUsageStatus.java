package no.imr.tools.swing.statusbar;

import com.google.common.html.HtmlEscapers;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.swing.GuiListeners;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class DiskUsageStatus {
   private @Nullable Path file;
   private final JProgressBar progressBar = new JProgressBar(0, 1000000) {
      @Override
      public String getToolTipText() {
         return makeTooltip();
      }
   };

   private long totalSpace;
   private long freeSpace;
   private @Nullable String error;
   private @Nullable Future<?> updateFuture;

   private DiskUsageStatus(@Nullable Path file) {
      this.file = file;

      ToolTipManager.sharedInstance().registerComponent(progressBar);
      progressBar.setStringPainted(true);
      progressBar.setMinimumSize(new Dimension(100, 0));
      progressBar.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            updateUpdating();
         }
      });
   }

   public DiskUsageStatus(FileParameter fileParameter) {
      this(fileParameter.getFile());

      fileParameter.subscribe(GuiListeners.coalescingLater(() -> setFile(fileParameter.getFile())));
   }

   private void updateUpdating() {
      if (updateFuture != null) {
         updateFuture.cancel(true);
         updateFuture = null;
      }
      if (file != null && progressBar.isShowing()) {
         updateFuture = Exec.scheduleWithFixedDelay(this::updateFileSpace, 0, 10, TimeUnit.SECONDS);
      }
      updateProgressBar();
   }

   private void updateFileSpace() {
      if (file == null) {
         return;
      }
      try {
         FileStore fileStore = getFileStore(file);
         totalSpace = fileStore.getTotalSpace();
         freeSpace = fileStore.getUnallocatedSpace();
         error = null;
      } catch (IOException e) {
         error = e.toString();
      }
      SwingUtilities.invokeLater(this::updateProgressBar);
   }

   private static FileStore getFileStore(Path file) throws IOException {
      try {
         return Files.getFileStore(file);
      } catch (IOException e) {
         if (FileUtils.notExists(e, file)) {
            Path existingParent = FileUtils.getExistingParent(file);
            if (existingParent != null) {
               return Files.getFileStore(existingParent);
            }
         }
         throw e;
      }
   }

   private void updateProgressBar() {
      if (file == null) {
         progressBar.setValue(0);
         progressBar.setString("");
         return;
      }
      if (error != null) {
         progressBar.setValue(0);
         progressBar.setString("Error");
         return;
      }
      long usedSpace = totalSpace - freeSpace;
      double x = usedSpace / (double) totalSpace;
      progressBar.setValue((int) (x * progressBar.getMaximum()));
      progressBar.setString(Utils.getByteSizeString(usedSpace) + " / " + Utils.getByteSizeString(totalSpace));
   }

   private String makeTooltip() {
      if (file == null) {
         return "No file to monitor";
      }
      String error = this.error;
      if (error != null) {
         return error;
      }
      long usedSpace = totalSpace - freeSpace;
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      dfs.setGroupingSeparator(' ');
      DecimalFormat df = new DecimalFormat(",###", dfs);
      return "<html>" + HtmlEscapers.htmlEscaper().escape(file.toString()) + "<br><br><table cellpadding=0>"
            + "<tr><td>Total space: &nbsp; </td><td align=right>" + df.format(totalSpace) + " bytes</td><td align=right> &nbsp; (" + Utils.getByteSizeString(totalSpace) + ")</td></tr>"
            + "<tr><td>Used space:</td><td align=right>" + df.format(usedSpace) + " bytes</td><td align=right>(" + Utils.getByteSizeString(usedSpace) + ")</td></tr>"
            + "<tr><td>Free space:</td><td align=right>" + df.format(freeSpace) + " bytes</td><td align=right>(" + Utils.getByteSizeString(freeSpace) + ")</td></tr>"
            + "</table>";
   }

   public void setFile(@Nullable Path file) {
      this.file = file;
      updateUpdating();
   }

   public JComponent getComponent() {
      return progressBar;
   }
}
