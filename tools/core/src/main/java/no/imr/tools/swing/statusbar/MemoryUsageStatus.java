package no.imr.tools.swing.statusbar;

import no.imr.tools.Utils;
import no.imr.tools.swing.WhenShowingTimer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.ToolTipManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public final class MemoryUsageStatus {
   private final JPanel panel = new JPanel(new BorderLayout());
   private final JProgressBar progressBar = new MemoryProgressBar();

   public MemoryUsageStatus() {
      ToolTipManager.sharedInstance().registerComponent(progressBar);
      progressBar.setStringPainted(true);
      progressBar.setMinimumSize(new Dimension(100, 0));

      WhenShowingTimer.start(progressBar, 1000, this::update);

      panel.add(progressBar);
   }

   private void update() {
      Runtime r = Runtime.getRuntime();
      long max = r.maxMemory();
      long free = r.freeMemory();
      long used = r.totalMemory() - free;
      double x = used / (double) max;
      progressBar.setValue((int) (x * progressBar.getMaximum()));
      progressBar.setString(getMemoryString(used) + " / " + getMemoryString(max));
   }

   private static String getMemoryString(long bytes) {
      long mb = bytes / (1024 * 1024);
      if (mb < 1000) {
         return mb + " MB";
      }
      double gb = mb / 1024.0;
      return Utils.numberToString(gb) + " GB";
   }

   public JComponent getComponent() {
      return panel;
   }

   private static final class MemoryProgressBar extends JProgressBar {
      private MemoryProgressBar() {
         super(0, 1000000);
      }

      @Override
      public String getToolTipText() {
         Runtime r = Runtime.getRuntime();
         long max = r.maxMemory();
         long free = r.freeMemory();
         long total = r.totalMemory();
         long used = total - free;
         long available = max - used;

         DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
         dfs.setGroupingSeparator(' ');
         DecimalFormat df = new DecimalFormat(",###", dfs);
         return "<html>Memory<br><br><table cellpadding=0>"
               + "<tr><td>Max memory:</td><td align=right>" + df.format(max) + " bytes</td><td align=right> &nbsp; (" + getMemoryString(max) + ")</td></tr>"
               + "<tr><td>Allocated memory: &nbsp; </td><td align=right>" + df.format(total) + " bytes</td><td align=right>(" + getMemoryString(total) + ")</td></tr>"
               + "<tr><td>Used memory:</td><td align=right>" + df.format(used) + " bytes</td><td align=right>(" + getMemoryString(used) + ")</td></tr>"
               + "<tr><td>Available memory:</td><td align=right>" + df.format(available) + " bytes</td><td align=right>(" + getMemoryString(available) + ")</td></tr>"
               + "</table>";
      }
   }
}
