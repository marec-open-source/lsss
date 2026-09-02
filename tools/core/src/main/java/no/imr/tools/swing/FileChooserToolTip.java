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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class FileChooserToolTip {
   private final JFileChooser fileChooser;
   private final Function<Path, @Nullable String> fileToToolTip;
   private final Map<Path, Optional<String>> fileToToolTipCache = new ConcurrentHashMap<>();
   private final CoalescingExecutor loader = new CoalescingExecutor(Exec.CACHED_THREAD_POOL);
   private volatile @Nullable Path file;

   private FileChooserToolTip(JFileChooser fileChooser, Function<Path, @Nullable String> fileToToolTip) {
      this.fileChooser = fileChooser;
      this.fileToToolTip = fileToToolTip;
      new DeepInputListener(fileChooser, new MouseAndKeyAdapter() {
         @Override
         public void mouseMoved(MouseEvent e) {
            update(e);
         }
      });
      fileChooser.addHierarchyListener(_ -> {
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
      Path updatedFile = file;
      String toolTip;
      if (updatedFile == null) {
         toolTip = null;
      } else if (fileToToolTipCache.get(updatedFile) instanceof Optional<String> opt) {
         toolTip = opt.orElse(null);
      } else {
         loader.execute(() -> {
            if (!updatedFile.equals(file) || fileToToolTipCache.containsKey(updatedFile)) {
               return;
            }
            String tip = fileToToolTip.apply(updatedFile);
            fileToToolTipCache.put(updatedFile, Optional.ofNullable(tip));
            SwingUtilities.invokeLater(() -> {
               if (updatedFile.equals(file)) {
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
      Object value = table.getModel().getValueAt(row, col);
      if (value == null) {
         return null;
      }
      return value.toString();
   }
}
