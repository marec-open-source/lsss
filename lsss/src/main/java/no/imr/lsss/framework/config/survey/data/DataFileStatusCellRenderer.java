package no.imr.lsss.framework.config.survey.data;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

final class DataFileStatusCellRenderer implements TableCellRenderer {
   static final long MODIFICATION_THRESHOLD_MILLIS = 60_000;

   private final DataFileTable dataFileTable;
   private @Nullable SvgIcon icon;
   private @Nullable SvgIcon frequencyIcon;

   private final JLabel label = new JLabel() {
      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         if (icon != null) {
            g.drawImage(icon.getImage(), 0, 0, dataFileTable);
         }
         if (frequencyIcon != null) {
            g.drawImage(frequencyIcon.getImage(), 0, 0, dataFileTable);
         }
      }
   };

   DataFileStatusCellRenderer(DataFileTable dataFileTable) {
      this.dataFileTable = dataFileTable;
   }

   @Override
   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      switch (value) {
         case DataFileTableModel.FileRow fileRow -> renderFileRow(fileRow);
         case DataFileTableModel.TimeRow timeRow -> renderTimeRow(timeRow);
         default -> {
            label.setText(null);
            icon = null;
            frequencyIcon = null;
         }
      }
      return label;
   }

   private void renderTimeRow(DataFileTableModel.TimeRow timeRow) {
      label.setText(timeRow.isExpanded() ? "‒" : "+");
      label.setHorizontalAlignment(JLabel.CENTER);
      icon = null;
      frequencyIcon = null;
   }

   private void renderFileRow(DataFileTableModel.FileRow fileRow) {
      SvgIcon newIcon = null;
      HtmlStringBuilder tooltip = dataFileTable.needToolTip() ? new HtmlStringBuilder() : null;
      if (tooltip != null) {
         tooltip.text(fileRow.getRawSegmentHandle().getDisplayName());

         SegmentInfo segmentInfo = fileRow.getSegmentInfo();
         if (segmentInfo != null) {
            DataFileNameCellRenderer.addFrequencyTooltip(tooltip, segmentInfo, fileRow.getDataTypes(DataType.RAW));
         }
      }

      // Errors:

      String dataExceptionMessage = fileRow.getDataExceptionMessage(DataType.RAW);
      if (dataExceptionMessage != null) {
         newIcon = LsssIcons.ERROR;
         if (tooltip != null) {
            tooltip.html("<br><br><span style='color: red;'><b>Error: </b></span>").text(dataExceptionMessage);
         }
      }

      String incompatibilityWithSelection = fileRow.getIncompatibilityWithSelection(DataType.RAW);
      if (incompatibilityWithSelection != null) {
         if (newIcon == null) {
            newIcon = LsssIcons.ERROR;
         }
         if (tooltip != null) {
            tooltip.html("<br><br><span style='color: red;'><b>Error: </b></span>Incompatible with selection: ").text(incompatibilityWithSelection);
         }
      }

      // Notices:

      List<String> notices = new ArrayList<>();

      if (fileRow == dataFileTable.getDataFileTableModel().getMostRecentlyModifiedRow(DataType.RAW)) {
         notices.add("The most recently modified file in the directory");
      }

      Instant lastModified = fileRow.getLastModified(DataType.RAW);
      if (lastModified != null && lastModified.until(Instant.now(), ChronoUnit.MILLIS) < MODIFICATION_THRESHOLD_MILLIS) {
         notices.add("Recently modified: " + TimeUtils.JAVA_UTIL_DATE_FORMATTER.format(lastModified));
      }

      DataFile rawDataFile = fileRow.getDataFile(DataType.RAW);
      if (rawDataFile != null) {
         notices.addAll(rawDataFile.getNotices());
      }

      if (notices.isEmpty()) {
         if (newIcon == null) {
            newIcon = rawDataFile == null ? null : LsssIcons.OK;
         }
      } else {
         if (newIcon == null) {
            newIcon = rawDataFile == null ? LsssIcons.NOTICE : LsssIcons.NOTICE_OK;
         }
         if (tooltip != null) {
            tooltip.html("<br>");
            for (String notice : notices) {
               tooltip.html("<br><span style='color: green;'><b>Notice: </b></span>").text(notice);
            }
         }
      }

      label.setText(null);
      icon = newIcon;
      frequencyIcon = getFrequencyIcon(fileRow);

      if (tooltip != null) {
         label.setToolTipText(tooltip.build());
      }
   }

   private @Nullable SvgIcon getFrequencyIcon(DataFileTableModel.FileRow fileRow) {
      if (fileRow.getDataFile(DataType.RAW) != null ||
            fileRow.getDataExceptionMessage(DataType.RAW) != null ||
            fileRow.getIncompatibilityWithSelection(DataType.RAW) != null) {
         return null;
      }
      SegmentInfo segmentInfo = fileRow.getSegmentInfo();
      if (segmentInfo == null) {
         return null;
      }
      DataFile firstRawDataFile = dataFileTable.getDataFileTableModel().getFirstRawDataFile();
      if (firstRawDataFile == null) {
         return null;
      }
      RawFileConfiguration firstRawFileConfiguration = firstRawDataFile.getPingConfiguration().getRawFileConfiguration();
      if (firstRawFileConfiguration.getIncompatibility(segmentInfo.rawFileConfigurationInfo()) != null) {
         return LsssIcons.FREQUENCY_ERROR;
      } else {
         return LsssIcons.FREQUENCY_OK;
      }
   }
}
