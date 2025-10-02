package no.imr.lsss.util.phantom;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.framework.config.survey.data.DataFileKoronaCellRenderer;
import no.imr.lsss.framework.config.survey.data.DataFileTable;
import no.imr.lsss.framework.config.survey.data.DataFileTableModel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

public final class PhantomCellRenderer implements TableCellRenderer {
   private final DataFileKoronaCellRenderer dataFileKoronaCellRenderer;
   private final PhantomDataAdministrator phantomDataAdministrator;

   public PhantomCellRenderer(DataFileTable dataFileTable, PhantomDataAdministrator phantomDataAdministrator) {
      dataFileKoronaCellRenderer = new DataFileKoronaCellRenderer(dataFileTable);
      this.phantomDataAdministrator = phantomDataAdministrator;
      dataFileTable.fixedWidthColumn(DataFileTableModel.KORONA_COLUMN, this, 26);
   }

   @Override
   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component koronaComponent = dataFileKoronaCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (value instanceof DataFileTableModel.FileRow fileRow) {
         return renderFileRow(koronaComponent, fileRow);
      }
      return koronaComponent;
   }

   private JComponent renderFileRow(Component koronaComponent, DataFileTableModel.FileRow fileRow) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBackground(Color.WHITE);
      panel.add(koronaComponent, BorderLayout.EAST);
      if (koronaComponent instanceof JComponent jComponent) {
         panel.setToolTipText(jComponent.getToolTipText());
      }

      SegmentHandle segmentHandle = fileRow.getRawSegmentHandle();
      SegmentHandle phantomSegmentHandle = phantomDataAdministrator.getRawToPhantom().get(segmentHandle);
      if (phantomSegmentHandle != null) {
         panel.add(new JLabel("P"), BorderLayout.WEST);
      }

      return panel;
   }
}
