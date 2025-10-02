package no.imr.tools.swing;

import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.table.TableUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class FileChooserToolTip {
   private final JFileChooser fileChooser;
   private final Function<Path, @Nullable String> fileToToolTip;
   private final Map<Path, @Nullable String> fileToToolTipCache = new HashMap<>();
   private final CoalescingExecutor loader = new CoalescingExecutor(Exec.CACHED_THREAD_POOL);
   private @Nullable Path file;

   private FileChooserToolTip(JFileChooser fileChooser, Function<Path, @Nullable String> fileToToolTip) {
      this.fileChooser = fileChooser;
      this.fileToToolTip = fileToToolTip;
      new DeepInputListener(fileChooser, new MouseAndKeyAdapter() {
         @Override
         public void mouseMoved(MouseEvent e) {
            update(e);
         }
      });
      fileChooser.addHierarchyListener(e -> {
         if (!fileChooser.isShowing()) {
            fileToToolTipCache.clear();
            file = null;
         }
      });
   }

   public static void install(JFileChooser fileChooser, Function<Path, @Nullable String> fileToToolTip) {
      new FileChooserToolTip(fileChooser, fileToToolTip);
   }

   private void update(MouseEvent e) {
      JComponent component;
      switch (e.getSource()) {
         case JList<?> list -> {
            component = list;
            file = FileUtils.toPath((File) GuiUtils.pointToListItem(list, e.getPoint()));
         }
         case JTable table -> {
            component = table;
            String fileName = pointToFileName(table, e.getPoint());
            file = fileName != null ? fileChooser.getCurrentDirectory().toPath().resolve(fileName) : null;
         }
         case null, default -> {
            file = null;
            return;
         }
      }
      String toolTip;
      if (file == null) {
         toolTip = null;
      } else if (fileToToolTipCache.containsKey(file)) {
         toolTip = fileToToolTipCache.get(file);
      } else {
         Path deferredFile = file;
         loader.execute(() -> {
            if (!deferredFile.equals(file) || fileToToolTipCache.containsKey(deferredFile)) {
               return;
            }
            String tip = fileToToolTip.apply(deferredFile);
            fileToToolTipCache.put(deferredFile, tip);
            SwingUtilities.invokeLater(() -> {
               if (deferredFile.equals(file)) {
                  component.setToolTipText(tip);
               }
            });
         });
         toolTip = null;
      }
      component.setToolTipText(toolTip);
   }

   private static @Nullable String pointToFileName(JTable table, Point point) {
      int row = TableUtils.pointToModelRow(table, point);
      if (row < 0) {
         return null;
      }
      int col = TableUtils.pointToModelColumn(table, point);
      if (col != 0) {
         return null;
      }
      return table.getModel().getValueAt(row, col).toString();
   }
}
