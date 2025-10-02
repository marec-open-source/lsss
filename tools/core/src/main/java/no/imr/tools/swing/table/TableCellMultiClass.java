package no.imr.tools.swing.table;

import org.jspecify.annotations.Nullable;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * For handling values of different classes in the cells in one table column.
 */
public final class TableCellMultiClass {
   private TableCellMultiClass() {
   }

   public static final class Renderer implements TableCellRenderer {
      private final Map<Class<?>, TableCellRenderer> renderers = new HashMap<>();

      public Renderer() {
      }

      public void addRenderer(Class<?> clazz, TableCellRenderer renderer) {
         renderers.put(clazz, renderer);
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         TableCellRenderer renderer = value != null ? getRenderer(value.getClass()) : null;
         if (renderer == null) {
            renderer = table.getDefaultRenderer(value != null ? value.getClass() : Object.class);
         }
         Component component = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
         component.setEnabled(table.isCellEditable(row, column));
         return component;
      }

      private @Nullable TableCellRenderer getRenderer(Class<?> clazz) {
         for (Map.Entry<Class<?>, TableCellRenderer> entry : renderers.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) {
               return entry.getValue();
            }
         }
         return null;
      }
   }
}
