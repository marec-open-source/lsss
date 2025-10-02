package no.imr.tools.swing.table;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public final class TableCellFloat {
   private TableCellFloat() {
   }

   public static final class Editor extends AbstractCellEditor implements TableCellEditor {
      private final JTextField textField = new JTextField();

      public Editor() {
         textField.setHorizontalAlignment(JTextField.RIGHT);
         textField.setBorder(null);
         textField.addPropertyChangeListener("value", evt -> fireEditingStopped());
         textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
               fireEditingStopped();
            }
         });
      }

      @Override
      public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
         textField.setText(value.toString());
         textField.selectAll();
         return textField;
      }

      @Override
      public Object getCellEditorValue() {
         return textField.getText();
      }
   }
}
