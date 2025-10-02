package no.imr.tools.swing.table;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;
import java.util.function.Supplier;

public final class TableCellCallbackEditor extends AbstractCellEditor implements TableCellEditor {
   private final Callback callback;
   private @Nullable Object cellEditorValue;
   private Supplier<@Nullable Component> renderer = () -> null;
   private final Component editorComponent = new Component() {
      @Override
      public void paint(Graphics g) {
         Component rendererComponent = renderer.get();
         if (rendererComponent == null) {
            return;
         }
         rendererComponent.setBounds(editorComponent.getBounds());
         rendererComponent.paint(g);
      }
   };

   public TableCellCallbackEditor(Callback callback) {
      this.callback = callback;
   }

   @Override
   public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      cellEditorValue = value;
      SwingUtilities.invokeLater(() -> {
         cellEditorValue = callback.edit(table, row, column);
         stopCellEditing();
      });
      renderer = () -> {
         TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
         return cellRenderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);
      };
      return editorComponent;
   }

   @Override
   public @Nullable Object getCellEditorValue() {
      return cellEditorValue;
   }

   @FunctionalInterface
   public interface Callback {
      @Nullable Object edit(JTable table, int row, int column);
   }
}
