package no.imr.tools.swing.table;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

public final class TableCellColorRenderer extends DefaultTableCellRenderer {
   private Color color = Color.WHITE;

   public TableCellColorRenderer() {
   }

   @Override
   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      color = (Color) value;
      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
   }

   @Override
   protected void paintComponent(Graphics g) {
      g.clearRect(0, 0, getWidth(), getHeight());
      g.setColor(color);
      g.fillRect(5, 2, getWidth() - 10, getHeight() - 4);
   }
}
