package no.imr.lsss.region.ek500;

import no.imr.korona.data.formats.ek500.EK500SegmentHandle;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

final class ConversionTableModel extends AbstractTableModel {
   private static final class Row {
      private final EK500SegmentHandle ek500SegmentHandle;
      private boolean selected = true;

      private Row(EK500SegmentHandle ek500SegmentHandle) {
         this.ek500SegmentHandle = ek500SegmentHandle;
      }
   }

   enum Column {
      Selected("Selected", Boolean.class) {
         @Override
         Object getValue(Row row) {
            return row.selected;
         }

         @Override
         void setValue(Object value, Row row) {
            row.selected = (Boolean) value;
         }
      },

      Segment("File", String.class) {
         @Override
         Object getValue(Row row) {
            return row.ek500SegmentHandle.getDisplayName();
         }
      };

      private final String columnName;
      private final Class<?> columnClass;

      Column(String columnName, Class<?> columnClass) {
         this.columnName = columnName;
         this.columnClass = columnClass;
      }

      abstract Object getValue(Row row);

      void setValue(Object value, Row row) {
         throw new UnsupportedOperationException(toString());
      }

      private static Column get(int i) {
         return values()[i];
      }
   }

   private final List<Row> rows = new ArrayList<>();

   ConversionTableModel(List<EK500SegmentHandle> ek500SegmentHandles) {
      for (EK500SegmentHandle ek500SegmentHandle : ek500SegmentHandles) {
         rows.add(new Row(ek500SegmentHandle));
      }
   }

   List<EK500SegmentHandle> getSelectedEK500SegmentHandles() {
      List<EK500SegmentHandle> ek500SegmentHandles = new ArrayList<>();
      for (Row row : rows) {
         if (row.selected) {
            ek500SegmentHandles.add(row.ek500SegmentHandle);
         }
      }
      return ek500SegmentHandles;
   }

   @Override
   public int getRowCount() {
      return rows.size();
   }

   @Override
   public String getColumnName(int column) {
      return Column.get(column).columnName;
   }

   @Override
   public Class<?> getColumnClass(int columnIndex) {
      return Column.get(columnIndex).columnClass;
   }

   @Override
   public int getColumnCount() {
      return Column.values().length;
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      Column column = Column.get(columnIndex);
      Row row = rows.get(rowIndex);
      return column.getValue(row);
   }

   @Override
   public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      Column column = Column.get(columnIndex);
      Row row = rows.get(rowIndex);
      column.setValue(aValue, row);
   }

   @Override
   public boolean isCellEditable(int rowIndex, int columnIndex) {
      Column column = Column.get(columnIndex);
      return column == Column.Selected;
   }
}
