package no.imr.tools.swing.table;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class TableCellButton {
   private TableCellButton() {
   }

   private static JButton createButton() {
      JButton button = new JButton();
      button.setMargin(new Insets(0, 0, 0, 0));
      return button;
   }

   public static final class ButtonSetting {
      private final String text;
      private final @Nullable String toolTip;

      public ButtonSetting(String text, @Nullable String toolTip) {
         this.text = text;
         this.toolTip = toolTip;
      }

      private void apply(JButton button) {
         button.setText(text);
         button.setToolTipText(toolTip);
      }
   }

   public static final class Renderer implements TableCellRenderer {
      private final JButton button = createButton();

      public Renderer() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         ButtonSetting buttonSetting = (ButtonSetting) value;
         buttonSetting.apply(button);
         button.setEnabled(table.isCellEditable(row, column));
         return button;
      }
   }

   public static final class Editor extends AbstractCellEditor implements TableCellEditor {
      private final JButton button = createButton();

      public Editor() {
         button.addActionListener(e -> fireEditingStopped());
         button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
               if (!button.getModel().isArmed()) {
                  fireEditingCanceled();
               }
            }
         });
      }

      @Override
      public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
         ButtonSetting buttonSetting = (ButtonSetting) value;
         buttonSetting.apply(button);
         return button;
      }

      @Override
      public @Nullable Object getCellEditorValue() {
         return null;
      }
   }
}
