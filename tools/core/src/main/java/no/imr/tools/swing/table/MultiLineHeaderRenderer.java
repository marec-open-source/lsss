package no.imr.tools.swing.table;

import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

/**
 * Class for rendering multiline JTable column headers.
 */
public final class MultiLineHeaderRenderer extends JList<String> implements TableCellRenderer {
   public MultiLineHeaderRenderer() {
      setForeground(UIManager.getColor("TableHeader.foreground"));
      setBackground(UIManager.getColor("TableHeader.background"));
      setBorder(UIManager.getBorder("TableHeader.cellBorder"));
      setFont(getFont().deriveFont(Font.PLAIN));
      ListCellRenderer<? super String> renderer = getCellRenderer();
      ((JLabel) renderer).setHorizontalAlignment(JLabel.CENTER);
   }

   @Override
   public Component getTableCellRendererComponent(JTable table, @Nullable Object value,
                                                  boolean isSelected, boolean hasFocus, int row, int column) {
      String stringValue = value != null ? value.toString() : "";
      String[] lines = stringValue.split("\\n");
      SortOrder sortOrder = getColumnSortOrder(table, column);
      String sortIcon;
      if (sortOrder == SortOrder.ASCENDING) {
         sortIcon = "▲";
      } else if (sortOrder == SortOrder.DESCENDING) {
         sortIcon = "▼";
      } else {
         sortIcon = null;
      }
      if (sortIcon != null) {
         if (lines.length > 0) {
            lines[0] = lines[0] + " " + sortIcon;
         } else {
            lines = new String[]{sortIcon};
         }
      }
      setListData(lines);
      return this;
   }

   private static @Nullable SortOrder getColumnSortOrder(JTable table, int column) {
      RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
      if (rowSorter != null) {
         List<? extends RowSorter.SortKey> sortKeys = rowSorter.getSortKeys();
         if (!sortKeys.isEmpty() && sortKeys.getFirst().getColumn() == table.convertColumnIndexToModel(column)) {
            return sortKeys.getFirst().getSortOrder();
         }
      }
      return null;
   }
}
