package no.imr.lsss.framework.config.survey.data;

import no.imr.korona.data.track.SegmentInfo;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.visualizer.ItemContainer;
import no.imr.tools.visualizer.ItemFeature;
import no.imr.tools.visualizer.ItemVisualizer;
import no.marec.lsss.api.util.GeoPoint;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.prefs.Preferences;

final class DataFileVisualizerDialog implements ItemContainer<DataFileTableModel.FileRow> {
   private final DataConf dataConf;
   private final ItemVisualizer<DataFileTableModel.FileRow> itemVisualizer;
   private Set<DataFileTableModel.FileRow> selectedItems = Set.of();

   DataFileVisualizerDialog(DataConf dataConf) {
      this.dataConf = dataConf;

      DateTimeFormatter timeFormat = Utils.createUTCDateTimeFormatter("yyyy-MM-dd HH:mm:ss");
      DecimalFormat geoPosFormat = Utils.createDecimalFormat("0.000000");
      DecimalFormat intFormat = Utils.createDecimalFormat("0");

      List<ItemFeature<DataFileTableModel.FileRow>> features = List.of(
            new ItemFeature.Text<>("File name", row -> {
               return row.getRawSegmentHandle().getDisplayName();
            }),
            ItemFeature.Time.fromInstant("Time", Unit.UTC, DataFileTableModel.FileRow::getInstant, timeFormat),
            new ItemFeature.Number<>("Duration", new Unit("Minutes"), rowSegmentInfo(segmentInfo -> {
               return segmentInfo.pingRange().getSeconds() / 60;
            }), Utils.createDecimalFormat("0.00")),
            new ItemFeature.Number<>("Ping count", Unit.COUNT, rowSegmentInfo(segmentInfo -> {
               return segmentInfo.pingRange().getPingCount();
            }), intFormat),
            new ItemFeature.Number<>("Vessel distance", Unit.NAUTICAL_MILES, rowSegmentInfo(segmentInfo -> {
               return segmentInfo.pingRange().getVesselDistance();
            }), Utils.createDecimalFormat("0.000")),
            new ItemFeature.Number<>("Vessel speed", Unit.KNOTS, rowSegmentInfo(segmentInfo -> {
               double hours = segmentInfo.pingRange().getSeconds() / 3600;
               return segmentInfo.pingRange().getVesselDistance() / hours;
            }), Utils.createDecimalFormat("0.00")),
            new ItemFeature.Number<>("Latitude", Unit.DEGREES, rowGeoPos(GeoPoint::getLatitude), geoPosFormat),
            new ItemFeature.Number<>("Longitude", Unit.DEGREES, rowGeoPos(GeoPoint::getLongitude), geoPosFormat),
            new ItemFeature.Number<>("Transducer count", Unit.COUNT, rowSegmentInfo(segmentInfo -> {
               return segmentInfo.rawFileConfigurationInfo().frequencies.length;
            }), intFormat),
            new ItemFeature.Number<>("<html>∑ s<sub>A</sub>", Unit.SA,
                  DataFileTableModel.FileRow::getSa, Utils.createDecimalFormat("0.000")),
            new ItemFeature.Number<>("File size", Unit.MEGABYTES, row -> {
               return row.getTotalFileSize(DataType.RAW) / (1024.0 * 1024.0);
            }, Utils.createDecimalFormat("0.000")),
            ItemFeature.Time.fromMillis("File last modified", Unit.UTC, row -> {
               return row.getLastModified(DataType.RAW);
            }, timeFormat)
      );

      itemVisualizer = new ItemVisualizer<>(features, this, Preferences.userRoot().node("/no/marec/lsss/DataFileVisualizerDialog"));
      WhenShowingListening.connect(itemVisualizer.getComponent(), dataConf.getFileTableChangeManager(), GuiListeners.coalescingLater(itemVisualizer::update));
      itemVisualizer.show(dataConf.getConfigurationManager().getDialog(), dataConf.getDisplayName());
   }

   private static ToDoubleFunction<DataFileTableModel.FileRow> rowSegmentInfo(ToDoubleFunction<SegmentInfo> segmentInfoToDouble) {
      return row -> {
         SegmentInfo segmentInfo = row.getSegmentInfo();
         return segmentInfo != null ? segmentInfoToDouble.applyAsDouble(segmentInfo) : Double.NaN;
      };
   }

   private static ToDoubleFunction<DataFileTableModel.FileRow> rowGeoPos(ToDoubleFunction<GeoPoint> geoPointToDouble) {
      return rowSegmentInfo(segmentInfo -> {
         GeoPoint geoPoint = segmentInfo.pingRange().begin().getGeographicalPosition();
         return geoPoint != null ? geoPointToDouble.applyAsDouble(geoPoint) : Double.NaN;
      });
   }

   @Override
   public Collection<DataFileTableModel.FileRow> getAllItems() {
      return dataConf.getDataFileTable().getDataFileTableModel().getFileRows();
   }

   @Override
   public Set<DataFileTableModel.FileRow> getSelectedItems() {
      return selectedItems;
   }

   @Override
   public void setSelectedItems(Set<DataFileTableModel.FileRow> items) {
      selectedItems = items;
      itemVisualizer.update();
   }
}
