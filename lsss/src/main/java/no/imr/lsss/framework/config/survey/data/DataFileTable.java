package no.imr.lsss.framework.config.survey.data;

import no.imr.tools.swing.ToolTipManagerState;
import no.imr.tools.swing.table.TableUtils;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public final class DataFileTable extends JTable {
   private boolean needToolTip;

   DataFileTable(DataFileTableModel dataFileTableModel) {
      super(dataFileTableModel);

      getTableHeader().setReorderingAllowed(false);

      DefaultTableCellRenderer rightAlignedRenderer = TableUtils.defaultTableCellRenderer(DefaultTableCellRenderer.RIGHT);

      fixedWidthColumn(DataFileTableModel.KORONA_COLUMN, new DataFileKoronaCellRenderer(this), 16);
      fixedWidthColumn(DataFileTableModel.STATUS_COLUMN, new DataFileStatusCellRenderer(this), 16);
      preferredWidthColumn(DataFileTableModel.FILE_NAME_COLUMN, new DataFileNameCellRenderer(this), 300);
      preferredWidthColumn(DataFileTableModel.PINGS_COLUMN, rightAlignedRenderer, 150);
      preferredWidthColumn(DataFileTableModel.DURATION_COLUMN, rightAlignedRenderer, 60);
      preferredWidthColumn(DataFileTableModel.DISTANCE_COLUMN, rightAlignedRenderer, 120);
      preferredWidthColumn(DataFileTableModel.SA_COLUMN, rightAlignedRenderer, 80);

      setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

      addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            Point point = e.getPoint();
            int columnIndex = TableUtils.pointToModelColumn(DataFileTable.this, point);
            if (columnIndex != DataFileTableModel.STATUS_COLUMN) {
               return;
            }
            int rowIndex = TableUtils.pointToModelRow(DataFileTable.this, point);
            if (rowIndex < 0) {
               return;
            }
            DataFileTableModel.BaseRow row = dataFileTableModel.getRow(rowIndex);
            if (row instanceof DataFileTableModel.TimeRow timeRow) {
               timeRow.setExpanded(!timeRow.isExpanded());
            }
         }
      });

      addMouseMotionListener(new MouseMotionAdapter() {
         private boolean previousAlwaysOn;

         @Override
         public void mouseMoved(MouseEvent e) {
            int column = TableUtils.pointToModelColumn(DataFileTable.this, e.getPoint());
            boolean alwaysOn = column == DataFileTableModel.STATUS_COLUMN || column == DataFileTableModel.KORONA_COLUMN;
            if (alwaysOn) {
               ToolTipManagerState.ALWAYS_ON.apply();
            } else {
               if (previousAlwaysOn) {
                  ToolTipManager.sharedInstance().setEnabled(false);
               }
               ToolTipManagerState.DEFAULT.apply();
            }
            previousAlwaysOn = alwaysOn;
         }
      });
   }

   DataFileTableModel getDataFileTableModel() {
      return (DataFileTableModel) getModel();
   }

   public void fixedWidthColumn(int columnIndex, TableCellRenderer tableCellRenderer, int fixedWidth) {
      TableColumn column = getColumnModel().getColumn(columnIndex);
      column.setCellRenderer(tableCellRenderer);
      column.setMaxWidth(fixedWidth);
      column.setMinWidth(fixedWidth);
   }

   private void preferredWidthColumn(int columnIndex, TableCellRenderer tableCellRenderer, int preferredWidth) {
      TableColumn fileNameColumn = getColumnModel().getColumn(columnIndex);
      fileNameColumn.setCellRenderer(tableCellRenderer);
      fileNameColumn.setPreferredWidth(preferredWidth);
   }

   @Override
   public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
      if (columnIndex == DataFileTableModel.STATUS_COLUMN) {
         return;
      }
      super.changeSelection(rowIndex, columnIndex, toggle, extend);
   }

   @Override
   public String getToolTipText(MouseEvent event) {
      needToolTip = true;
      String toolTipText = super.getToolTipText(event);
      needToolTip = false;
      return toolTipText;
   }

   boolean needToolTip() {
      return needToolTip;
   }
}
