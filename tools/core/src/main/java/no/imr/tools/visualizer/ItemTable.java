package no.imr.tools.visualizer;

import no.imr.tools.swing.table.MultiLineHeaderRenderer;
import no.imr.tools.swing.table.TableUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;

final class ItemTable<T> extends ItemView<T> {
   private final List<ItemFeature<T>> features;
   private final ItemContainer<T> itemContainer;
   private final JScrollPane scrollPane;
   private final JTable table;
   private final ItemTableModel<T> model;
   private boolean updating;

   ItemTable(List<ItemFeature<T>> features, ItemContainer<T> itemContainer) {
      this.features = features;
      this.itemContainer = itemContainer;

      model = new ItemTableModel<>(features);
      table = new MyTable();
      table.setAutoCreateRowSorter(true);

      table.getTableHeader().setDefaultRenderer(new MultiLineHeaderRenderer());

      DefaultTableCellRenderer tableCellRenderer = new DefaultTableCellRenderer() {
         @Override
         public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int modelColumn = table.convertColumnIndexToModel(column);
            int modelRow = table.convertRowIndexToModel(row);
            String string = features.get(modelColumn).itemToString.apply(model.getItems().get(modelRow));
            return super.getTableCellRendererComponent(table, string, isSelected, hasFocus, row, column);
         }
      };
      tableCellRenderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
      table.setDefaultRenderer(Double.class, tableCellRenderer);

      table.getSelectionModel().addListSelectionListener(e -> {
         if (updating || e.getValueIsAdjusting()) {
            return;
         }
         updateItemSelection();
      });

      scrollPane = new JScrollPane(table);
   }

   @Override
   JComponent getComponent() {
      return scrollPane;
   }

   @Override
   void selectFeatures(ItemFeature<T> x, ItemFeature<T> y) {
   }

   @Override
   void update() {
      updating = true;
      try {
         model.update(itemContainer);
         updateTableSelection();
      } finally {
         updating = false;
      }
   }

   private void updateItemSelection() {
      Set<T> selectedItems = TableUtils.getSelectedItems(table, model.getItems());
      itemContainer.setSelectedItems(selectedItems);
   }

   private void updateTableSelection() {
      Set<T> selectedItems = itemContainer.getSelectedItems();
      TableUtils.setTableSelection(table, model.getItems(), selectedItems);
   }

   private final class MyTable extends JTable {
      private MyTable() {
         super(model);
      }

      @Override
      protected JTableHeader createDefaultTableHeader() {
         return new JTableHeader(getColumnModel()) {
            @Override
            public @Nullable String getToolTipText(MouseEvent event) {
               int column = TableUtils.pointToModelColumn(getTable(), event.getPoint());
               return column >= 0 ? features.get(column).getNameAndUnit() : null;
            }
         };
      }

      @Override
      public @Nullable String getToolTipText(MouseEvent event) {
         int row = TableUtils.pointToModelRow(table, event.getPoint());
         return row >= 0 ? ItemUtils.getToolTipText(model.getItems().get(row), features) : null;
      }
   }
}
