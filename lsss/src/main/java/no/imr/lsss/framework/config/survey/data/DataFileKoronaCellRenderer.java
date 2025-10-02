package no.imr.lsss.framework.config.survey.data;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.svg.SvgIcon;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public final class DataFileKoronaCellRenderer implements TableCellRenderer {
   private final DataFileTable dataFileTable;
   private final JLabel label = new JLabel();

   public DataFileKoronaCellRenderer(DataFileTable dataFileTable) {
      this.dataFileTable = dataFileTable;
   }

   @Override
   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value instanceof DataFileTableModel.FileRow fileRow) {
         return renderFileRow(table, fileRow, row);
      } else {
         return emptyLabel();
      }
   }

   private JComponent emptyLabel() {
      label.setIcon(null);
      label.setText(null);
      label.setToolTipText(null);
      return label;
   }

   private JComponent renderFileRow(JTable table, DataFileTableModel.FileRow fileRow, int row) {
      SegmentHandle processedSegmentHandle = fileRow.getSegmentHandle(DataType.PROCESSED);
      if (processedSegmentHandle == null) {
         return emptyLabel();
      }

      SvgIcon icon = null;
      HtmlStringBuilder tooltip = dataFileTable.needToolTip() ? new HtmlStringBuilder() : null;
      if (tooltip != null) {
         tooltip.text(processedSegmentHandle.getDisplayName());
      }

      // Errors:

      String dataExceptionMessage = fileRow.getDataExceptionMessage(DataType.PROCESSED);
      if (dataExceptionMessage != null) {
         icon = LsssIcons.KORONA_ERROR;
         if (tooltip != null) {
            tooltip.html("<br><br><span style='color: red;'><b>Error: </b></span>").text(dataExceptionMessage);
         }
      }

      String incompatibilityWithSelection = fileRow.getIncompatibilityWithSelection(DataType.PROCESSED);
      if (incompatibilityWithSelection != null) {
         if (icon == null) {
            icon = LsssIcons.KORONA_ERROR;
         }
         if (tooltip != null) {
            tooltip.html("<br><br><span style='color: red;'><b>Error: </b></span>Incompatible with selection: ").text(incompatibilityWithSelection);
         }
      }

      // Notices:

      List<String> notices = new ArrayList<>();

      if (row == table.getRowCount() - 1) {
         notices.add("Last file in directory");
      }

      long lastModified = fileRow.getLastModified(DataType.PROCESSED);
      if (lastModified + DataFileStatusCellRenderer.MODIFICATION_THRESHOLD > System.currentTimeMillis()) {
         notices.add("Recently modified: " + new Date(lastModified));
      }

      if (!fileRow.getKoronaConflictingFiles().isEmpty()) {
         String fileList = fileRow.getKoronaConflictingFiles().stream()
               .map(file -> "<li>" + HtmlEscapers.htmlEscaper().escape(file) + "</li>")
               .collect(Collectors.joining());
         notices.add("Conflicting KORONA files:<ul>" + fileList + "</ul>");
      }

      DataFile processedDataFile = fileRow.getDataFile(DataType.PROCESSED);
      if (processedDataFile != null) {
         notices.addAll(processedDataFile.getNotices());
      }

      if (!notices.isEmpty()) {
         if (icon == null) {
            icon = processedDataFile == null ? LsssIcons.KORONA : LsssIcons.KORONA_NOTICE_OK;
         }
         if (tooltip != null) {
            tooltip.html("<br>");
            for (String notice : notices) {
               tooltip.html("<br><span style='color: green;'><b>Notice: </b></span>").text(notice);
            }
         }
      } else {
         if (icon == null) {
            icon = processedDataFile == null ? LsssIcons.KORONA : LsssIcons.KORONA_OK;
         }
      }

      icon.on(label);
      label.setToolTipText(tooltip != null ? tooltip.build() : null);
      return label;
   }
}
