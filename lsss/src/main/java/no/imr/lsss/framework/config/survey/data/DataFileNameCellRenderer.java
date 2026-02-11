package no.imr.lsss.framework.config.survey.data;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.datamanager.labelling.DataFileLabel;
import no.imr.korona.data.datamanager.labelling.DataFileLabelUtils;
import no.imr.korona.data.datamanager.labelling.DataFileLabelling;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.tools.Utils;
import no.imr.tools.misc.HtmlStringBuilder;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

final class DataFileNameCellRenderer extends DefaultTableCellRenderer {
   private final DataFileTable dataFileTable;

   DataFileNameCellRenderer(DataFileTable dataFileTable) {
      this.dataFileTable = dataFileTable;
   }

   @Override
   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      return switch (value) {
         case DataFileTableModel.FileRow fileRow -> renderFileRow(table, fileRow, isSelected, hasFocus, row, column);
         case DataFileTableModel.TimeRow timeRow -> renderTimeRow(table, timeRow, isSelected, hasFocus, row, column);
         default -> super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      };
   }

   private Component renderTimeRow(JTable table, DataFileTableModel.TimeRow timeRow, boolean isSelected, boolean hasFocus, int row, int column) {
      String time = timeRow.isUnknownTime()
            ? "Unknown time"
            : dataFileTable.getDataFileTableModel().getDataConf().timeGrouping.getValue().dateTimeFormatter.format(timeRow.getTimeGroup().instant());

      DataFileLabelling dataFileLabelling = dataFileTable.getDataFileTableModel().getDataConf().getDataFileLabelling();
      Collection<DataFileLabel> labels;
      if (dataFileLabelling != null) {
         labels = timeRow.getFileRows().stream()
               .flatMap(fileRow -> dataFileLabelling.getLabels(fileRow.getRawSegmentHandle()).stream())
               .collect(Collectors.toCollection(TreeSet::new));
      } else {
         labels = List.of();
      }

      String text = DataFileLabelUtils.addLabelsText(time, labels);

      JComponent component = (JComponent) super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
      if (dataFileTable.needToolTip()) {
         long totalSize = timeRow.getFileRows().stream()
               .mapToLong(r -> r.getTotalFileSize(DataType.RAW))
               .sum();
         HtmlStringBuilder tooltip = new HtmlStringBuilder()
               .text(time)
               .html("<br>Files: ").text(timeRow.getFileRows().size())
               .html("<br>Size: ").text(Utils.getByteSizeString(totalSize));
         DataFileLabelUtils.addLabelsTooltip(tooltip, labels);
         component.setToolTipText(tooltip.build());
      }
      return component;
   }

   private Component renderFileRow(JTable table, DataFileTableModel.FileRow fileRow, boolean isSelected, boolean hasFocus, int row, int column) {
      if (fileRow.getDataExceptionMessage(DataType.RAW) != null || fileRow.getIncompatibilityWithSelection(DataType.RAW) != null) {
         isSelected = false;
      }

      SegmentHandle segmentHandle = fileRow.getRawSegmentHandle();

      DataFileLabelling dataFileLabelling = dataFileTable.getDataFileTableModel().getDataConf().getDataFileLabelling();
      Collection<DataFileLabel> labels = dataFileLabelling != null
            ? Utils.sorted(dataFileLabelling.getLabels(segmentHandle))
            : List.of();

      String text = DataFileLabelUtils.addLabelsText(segmentHandle.toString(), labels);

      JComponent component = (JComponent) super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);

      if (dataFileTable.needToolTip()) {
         HtmlStringBuilder tooltip = new HtmlStringBuilder()
               .text(fileRow.getRawSegmentHandle().getDisplayName())
               .html("<br>Size: ").text(Utils.getByteSizeString(fileRow.getTotalFileSize(DataType.RAW)));
         SegmentInfo segmentInfo = fileRow.getSegmentInfo();
         if (segmentInfo != null && !segmentInfo.pingRange().isEmpty()) {
            tooltip.html("<br>Start: ").text(new Date(segmentInfo.pingRange().begin().getTimeInMillis()).toString());
            addFrequencyTooltip(tooltip, segmentInfo, fileRow.getDataTypes(DataType.RAW));
         }
         DataFileLabelUtils.addLabelsTooltip(tooltip, labels);
         component.setToolTipText(tooltip.build());
      }

      return component;
   }

   static void addFrequencyTooltip(HtmlStringBuilder tooltip, SegmentInfo segmentInfo, ImmutableMap<Integer, String> dataTypes) {
      tooltip.html("<br>Freq: ");
      float[] frequencies = segmentInfo.rawFileConfigurationInfo().frequencies;
      for (int i = 0; i < frequencies.length; i++) {
         if (i > 0) {
            tooltip.text(", ");
         }
         if (i >= 20) {
            tooltip.text("...");
            break;
         }
         tooltip.text(Utils.hzToKHz(frequencies[i]));
         if (BroadbandData.BROADBAND_DATA_TYPE_NAME.equals(dataTypes.get(i + 1))) {
            tooltip.text(" FM");
         }
      }
      tooltip.text(" kHz");
   }
}
