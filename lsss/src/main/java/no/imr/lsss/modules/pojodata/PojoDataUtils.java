package no.imr.lsss.modules.pojodata;

import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.util.ExportRounding;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.plot.XYInfoContainer;
import no.imr.tools.plot.XYZInfo;
import no.imr.tools.plot.XYZInfoContainer;
import no.marec.lsss.api.util.GeoPoint;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class PojoDataUtils {
   public static final String DATASETS = "datasets";
   public static final String DATASET_NAME = "name";

   private PojoDataUtils() {
   }

   static List<PojoData> toPojoDatasets(PojoData.Builder builder, XYPlot plot) {
      List<PojoData> datasets = new ArrayList<>();
      int datasetCount = plot.getDatasetCount();
      for (int datasetIndex = 0; datasetIndex < datasetCount; datasetIndex++) {
         XYDataset dataset = plot.getDataset(datasetIndex);
         if (dataset == null) {
            continue;
         }
         int seriesCount = dataset.getSeriesCount();
         for (int seriesIndex = 0; seriesIndex < seriesCount; seriesIndex++) {
            PojoData pojoDataset = toPojoDataset(builder, dataset, seriesIndex);
            if (pojoDataset != null) {
               datasets.add(pojoDataset);
            }
         }
      }
      return datasets;
   }

   private static @Nullable PojoData toPojoDataset(PojoData.Builder builder, XYDataset dataset, int seriesIndex) {
      if (dataset instanceof XYZDataset xyzDataset) {
         return toPojoDataset(builder, xyzDataset, seriesIndex);
      }
      XYInfo xyInfo = dataset instanceof XYInfoContainer xyInfoContainer
            ? xyInfoContainer.getXYInfo(seriesIndex)
            : null;
      if (xyInfo != null) {
         if (xyInfo.isEmpty()) {
            return null;
         }
      } else {
         xyInfo = new XYInfo(
               new ParameterExport("x", Unit.NONE, ExportTransform.identity()),
               new ParameterExport("y", Unit.NONE, ExportTransform.identity()));
      }
      int itemCount = dataset.getItemCount(seriesIndex);
      return builder.newBuilder()
            .with(DATASET_NAME, dataset.getSeriesKey(seriesIndex).toString())
            .withCoordinateVariable(xyInfo.x())
            .withDataVariable(xyInfo.y())
            .with(xyInfo.x(), IntStream.range(0, itemCount).mapToDouble(itemIndex -> dataset.getXValue(seriesIndex, itemIndex)))
            .with(xyInfo.y(), IntStream.range(0, itemCount).mapToDouble(itemIndex -> dataset.getYValue(seriesIndex, itemIndex)))
            .build();
   }

   private static @Nullable PojoData toPojoDataset(PojoData.Builder builder, XYZDataset dataset, int seriesIndex) {
      XYZInfo xyzInfo = dataset instanceof XYZInfoContainer xyzInfoContainer
            ? xyzInfoContainer.getXYZInfo(seriesIndex)
            : null;
      if (xyzInfo != null) {
         if (xyzInfo.isEmpty()) {
            return null;
         }
      } else {
         xyzInfo = new XYZInfo(
               new ParameterExport("x", Unit.NONE, ExportTransform.identity()),
               new ParameterExport("y", Unit.NONE, ExportTransform.identity()),
               new ParameterExport("z", Unit.NONE, ExportTransform.identity()));
      }
      int itemCount = dataset.getItemCount(seriesIndex);
      return builder.newBuilder()
            .with(DATASET_NAME, dataset.getSeriesKey(seriesIndex).toString())
            .withCoordinateVariable(xyzInfo.x(), xyzInfo.y())
            .withDataVariable(xyzInfo.z())
            .with(xyzInfo.x(), IntStream.range(0, itemCount).mapToDouble(itemIndex -> dataset.getXValue(seriesIndex, itemIndex)))
            .with(xyzInfo.y(), IntStream.range(0, itemCount).mapToDouble(itemIndex -> dataset.getYValue(seriesIndex, itemIndex)))
            .with(xyzInfo.z(), IntStream.range(0, itemCount).mapToDouble(itemIndex -> dataset.getZValue(seriesIndex, itemIndex)))
            .build();
   }

   public static double getLongitudeOrNaN(@Nullable GeoPoint geoPos) {
      return geoPos != null ? geoPos.getLongitude() : Double.NaN;
   }

   public static double getLatitudeOrNaN(@Nullable GeoPoint geoPos) {
      return geoPos != null ? geoPos.getLatitude() : Double.NaN;
   }

   public static ParameterExport getParameterExport(PingMapping pingMapping) {
      return switch (pingMapping) {
         case NUMBER -> new ParameterExport("pingNumber", Unit.COUNT, ExportTransform.identity());
         case DISTANCE -> new ParameterExport("vesselDistance", Unit.NAUTICAL_MILES, ExportRounding.vesselDistance());
         case TIME -> new ParameterExport("time", Unit.SECONDS_SINCE_EPOCH, ExportTransform.identity());
      };
   }
}
