package no.imr.tools.swing.table;

import no.imr.tools.parameter.ParameterException;
import no.imr.tools.parameter.gui.input.ParameterGuiUtils;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TableUtils {
   private TableUtils() {
   }

   // Workaround for https://bugs.openjdk.org/browse/JDK-6291631
   public static int rowAtPoint(JTable table, Point point) {
      return point.y < 0 ? -1 : table.rowAtPoint(point);
   }

   public static int pointToModelRow(JTable table, Point point) {
      int row = rowAtPoint(table, point);
      return row < 0 ? row : table.convertRowIndexToModel(row);
   }

   public static int pointToModelColumn(JTable table, Point point) {
      int col = table.columnAtPoint(point);
      return col < 0 ? col : table.convertColumnIndexToModel(col);
   }

   public static void stopCellEditing(JTable table) {
      TableCellEditor cellEditor = table.getCellEditor();
      if (cellEditor != null) {
         cellEditor.stopCellEditing();
      }
   }

   public static DefaultTableCellRenderer defaultTableCellRenderer(int horizontalAlignment) {
      DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
      renderer.setHorizontalAlignment(horizontalAlignment);
      return renderer;
   }

   public static void scrollToSelectedRows(JTable table) {
      int min = table.getSelectionModel().getMinSelectionIndex();
      int max = table.getSelectionModel().getMaxSelectionIndex();
      if (min >= 0 && max >= 0) {
         scrollToRows(table, min, max);
      }
   }

   public static void scrollToRows(JTable table, int firstRow, int lastRow) {
      Rectangle targetRectangle = table.getCellRect(firstRow, 0, true);
      targetRectangle.add(table.getCellRect(lastRow, 0, true));
      Rectangle visibleRect = table.getVisibleRect();
      if (!visibleRect.contains(targetRectangle)) {
         targetRectangle.height = visibleRect.height;
         table.scrollRectToVisible(targetRectangle);
      }
   }

   public static void setTableSelection(JTable table, IntPredicate rowPredicate) {
      table.getSelectionModel().setValueIsAdjusting(true);
      table.clearSelection();
      int rowCount = table.getRowCount();
      for (int i = 0; i < rowCount; i++) {
         if (rowPredicate.test(i)) {
            int row = table.convertRowIndexToView(i);
            table.addRowSelectionInterval(row, row);
         }
      }
      table.getSelectionModel().setValueIsAdjusting(false);
   }

   public static <T> boolean setTableSelection(JTable table, List<T> allItems, Set<T> selectedItems) {
      if (getSelectedItems(table, allItems).equals(selectedItems)) {
         return false;
      }
      setTableSelection(table, i -> selectedItems.contains(allItems.get(i)));
      return true;
   }

   public static <T> Set<T> getSelectedItems(JTable table, List<T> allItems) {
      ListSelectionModel selectionModel = table.getSelectionModel();
      return IntStream.rangeClosed(selectionModel.getMinSelectionIndex(), selectionModel.getMaxSelectionIndex())
            .filter(selectionModel::isSelectedIndex)
            .map(table::convertRowIndexToModel)
            .mapToObj(allItems::get)
            .collect(Collectors.toSet());
   }

   public static int[] getSelectedModelRows(JTable table) {
      ListSelectionModel selectionModel = table.getSelectionModel();
      int[] rows = IntStream.rangeClosed(selectionModel.getMinSelectionIndex(), selectionModel.getMaxSelectionIndex())
            .filter(selectionModel::isSelectedIndex)
            .map(table::convertRowIndexToModel)
            .toArray();
      Arrays.sort(rows);
      return rows;
   }

   public static void updateAllRows(AbstractTableModel tableModel) {
      int rowCount = tableModel.getRowCount();
      if (rowCount > 0) {
         tableModel.fireTableRowsUpdated(0, rowCount - 1);
      }
   }

   public static void copyToClipboard(JTable table) {
      int[] selectedRows = table.getSelectedRows();
      int[] selectedColumns = table.getSelectedColumns();
      String data = IntStream.of(selectedRows)
            .mapToObj(row -> IntStream.of(selectedColumns)
                  .mapToObj(column -> Objects.toString(table.getValueAt(row, column), ""))
                  .collect(Collectors.joining("\t")))
            .collect(Collectors.joining("\n"));
      StringSelection stringSelection = new StringSelection(data);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
   }

   public static void pasteFromClipboard(JTable table) {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
         JOptionPane.showMessageDialog(table, "Cannot paste clipboard content type");
         return;
      }
      String data;
      try {
         data = (String) clipboard.getData(DataFlavor.stringFlavor);
      } catch (Exception e) {
         JOptionPane.showMessageDialog(table, "Error getting clipboard content:\n" + e);
         return;
      }
      AbstractTableModel tableModel = (AbstractTableModel) table.getModel();
      List<String> lines = data.lines().toList();
      int sourceColumnCount = lines.stream().mapToInt(line -> line.split("\t").length).max().orElse(1);
      int[] targetRows = table.getSelectedRows();
      int[] targetColumns = table.getSelectedColumns();
      if (targetRows.length == 1 && targetColumns.length == 1) {
         targetRows = IntStream.range(targetRows[0], table.getRowCount()).limit(lines.size()).toArray();
         targetColumns = IntStream.range(targetColumns[0], table.getColumnCount()).limit(sourceColumnCount).toArray();
      }
      for (int i = 0; i < targetRows.length; i++) {
         String line;
         if (lines.size() == 1) {
            line = lines.getFirst();
         } else if (i < lines.size()) {
            line = lines.get(i);
         } else {
            break;
         }
         String[] words = line.split("\t");
         int row = targetRows[i];
         for (int j = 0; j < targetColumns.length; j++) {
            String word;
            if (words.length == 1) {
               word = words[0];
            } else if (j < words.length) {
               word = words[j];
            } else {
               break;
            }
            int column = targetColumns[j];
            try {
               tableModel.setValueAt(word, row, column);
            } catch (ParameterException e) {
               ParameterGuiUtils.showErrorDialog(e, table);
               return;
            }
         }
         tableModel.fireTableRowsUpdated(row, row);
      }
   }

   public static void setValueAtSelection(JTable table, String value) {
      AbstractTableModel tableModel = (AbstractTableModel) table.getModel();
      int[] selectedRows = table.getSelectedRows();
      int[] selectedColumns = table.getSelectedColumns();
      for (int row : selectedRows) {
         for (int column : selectedColumns) {
            try {
               tableModel.setValueAt(value, row, column);
            } catch (ParameterException e) {
               ParameterGuiUtils.showErrorDialog(e, table);
               return;
            }
         }
         tableModel.fireTableRowsUpdated(row, row);
      }
   }
}
