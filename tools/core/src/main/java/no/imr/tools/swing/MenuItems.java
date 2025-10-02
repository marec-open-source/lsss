package no.imr.tools.swing;

import no.imr.tools.Utils;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MenuItems {
   private MenuItems() {
   }

   public static JLabel label(SvgIcon icon, String text, boolean bold) {
      JLabel label = icon.on(new JLabel(text));
      label.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 0));
      if (bold) {
         label.setFont(label.getFont().deriveFont(Font.BOLD));
      }
      return label;
   }

   public static JMenuItem about(ApplicationInfo applicationInfo) {
      JMenuItem item = new JMenuItem("About...");
      item.setMnemonic(KeyEvent.VK_A);
      item.addActionListener(e -> AboutDialog.show(GuiUtils.windowForEvent(e), applicationInfo));
      return item;
   }

   public static JMenuItem releaseNotes(Path installationDir) {
      Path releaseNotesDir = Utils.IS_DIST_VERSION
            ? installationDir.getParent()
            : installationDir.resolve("src").resolve("dist");
      JMenuItem item = desktopOpen("Show release notes", releaseNotesDir.resolve("ReleaseNotes.txt"));
      item.setMnemonic(KeyEvent.VK_R);
      return item;
   }

   public static JMenuItem logFile(LoggingManager loggingManager) {
      JMenuItem item = desktopOpen("Show log file", loggingManager.getLogFile());
      item.setMnemonic(KeyEvent.VK_L);
      return item;
   }

   public static JMenuItem desktopOpen(String text, @Nullable Path file) {
      JMenuItem item = new JMenuItem(text);
      if (file != null) {
         item.addActionListener(e -> GuiUtils.desktopOpen(file, GuiUtils.windowForEvent(e)));
      } else {
         item.setEnabled(false);
      }
      return item;
   }

   public static JMenuItem showInFileExplorer(@Nullable Path file) {
      JMenuItem item = new JMenuItem("Show in file explorer");
      item.setEnabled(false);
      if (file != null) {
         Exec.CACHED_THREAD_POOL.execute(() -> {
            Path fileToOpen = Files.isRegularFile(file) ? file.getParent() : file;
            if (fileToOpen == null || !Files.exists(fileToOpen)) {
               return;
            }
            SwingUtilities.invokeLater(() -> {
               item.setEnabled(true);
               item.addActionListener(e -> GuiUtils.desktopOpen(fileToOpen, GuiUtils.windowForEvent(e)));
            });
         });
      }
      return item;
   }
}
