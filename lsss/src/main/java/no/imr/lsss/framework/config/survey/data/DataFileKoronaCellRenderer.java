package no.imr.lsss.framework.config.survey.data;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.time.TimeUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
         return renderFileRow(fileRow);
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

   private JComponent renderFileRow(DataFileTableModel.FileRow fileRow) {
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
         boolean warning = dataFileTable.getDataFileTableModel().getDataConf().canUpdateSelectedDataFilesWithIncompatiblePingConfiguration();
         if (icon == null) {
            icon = warning ? LsssIcons.KORONA_NOTICE_OK : LsssIcons.KORONA_ERROR;
         }
         if (tooltip != null) {
            String color = warning ? "yellow" : "red";
            String type = warning ? "Warning" : "Error";
            tooltip.html("<br><br><span style='color: " + color + ";'><b>" + type + ": </b></span>Incompatible with selection: ").text(incompatibilityWithSelection);
         }
      }

      // Notices:

      List<String> notices = new ArrayList<>();

      if (fileRow == dataFileTable.getDataFileTableModel().getMostRecentlyModifiedRow(DataType.PROCESSED)) {
         notices.add("The most recently modified file in the directory");
      }

      Instant lastModified = fileRow.getLastModified(DataType.PROCESSED);
      if (lastModified != null && lastModified.until(Instant.now(), ChronoUnit.MILLIS) < DataFileStatusCellRenderer.MODIFICATION_THRESHOLD_MILLIS) {
         notices.add("Recently modified: " + TimeUtils.JAVA_UTIL_DATE_FORMATTER.format(lastModified));
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
