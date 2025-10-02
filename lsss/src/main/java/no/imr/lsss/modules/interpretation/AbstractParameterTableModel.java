package no.imr.lsss.modules.interpretation;

import no.imr.tools.swing.table.TableCellSlider;

import javax.swing.table.AbstractTableModel;

/**
 * Model for table with parameters, such as bubble correction.
 */
public abstract class AbstractParameterTableModel extends AbstractTableModel {
   public static final int NAME_COLUMN = 0;
   public static final int SLIDER_COLUMN = 1;
   public static final int VALUE_COLUMN = 2;

   private final int rowCount;

   protected AbstractParameterTableModel(int rowCount) {
      this.rowCount = rowCount;
   }

   @Override
   public int getColumnCount() {
      return 3;
   }

   @Override
   public String getColumnName(int column) {
      return "";
   }

   @Override
   public Class<?> getColumnClass(int columnIndex) {
      return switch (columnIndex) {
         case NAME_COLUMN -> String.class;
         case SLIDER_COLUMN -> TableCellSlider.SliderSetting.class;
         case VALUE_COLUMN -> Float.class;
         default -> throw new IllegalArgumentException(String.valueOf(columnIndex));
      };
   }

   @Override
   public int getRowCount() {
      return rowCount;
   }

   @Override
   public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex > 0;
   }
}
