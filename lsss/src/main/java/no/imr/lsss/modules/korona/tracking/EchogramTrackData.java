package no.imr.lsss.modules.korona.tracking;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.concurrent.ConcurrentObject;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.listening.ObservableChangeManager;
import no.imr.tools.swing.Drawable;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.util.LineStripBuilder;
import no.marec.lsss.api.util.observing.Observable;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class EchogramTrackData extends ConcurrentObject {
   private final TrackInfoModule trackInfoModule;
   private final EchogramModule echogramModule;
   private final InterpretationSettings interpretationSettings;
   private Map<TrackId, TrackData> trackData = Map.of();
   private final ObservableChangeManager<Map<TrackId, TrackData>> trackDataChangeManager = new ObservableChangeManager<>(this::updateEnabledState);

   EchogramTrackData(TrackInfoModule trackInfoModule, EchogramModule echogramModule) {
      super(trackInfoModule.getLSSS().getInterpretationSettings().createObservingSerialExecutor());

      this.trackInfoModule = trackInfoModule;
      this.echogramModule = echogramModule;
      interpretationSettings = trackInfoModule.getLSSS().getInterpretationSettings();
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::recompute), List.of(
            trackInfoModule.getValidIdsChangeManager(),
            trackInfoModule.getTrackLabelling().getChangeManager(),
            echogramModule.echogramArea(),
            interpretationSettings.getChannelChangeManager(),
            interpretationSettings.getPingSampler().getNewPingsChangeManager()
      ));

      //---

      recompute();
   }

   Map<TrackId, TrackData> getTrackData() {
      return trackData;
   }

   Observable<Map<TrackId, TrackData>> getTrackDataChangeManager() {
      return trackDataChangeManager;
   }

   private void updateEnabledState() {
      setEnabled(!trackDataChangeManager.isEmpty());
   }

   private void recompute() {
      trackData = createTrackData();
      trackDataChangeManager.notifyListeners(trackData);
   }

   private Map<TrackId, TrackData> createTrackData() {
      return createTrackData(trackInfoModule.getTrackEditing()::getTrackBorders);
   }

   Map<TrackId, TrackData> createTrackData(TrackBorderExtractor trackBorderExtractor) {
      DataFileSet dataFileSet = interpretationSettings.getDataFileSet();
      int channel = interpretationSettings.getChannel();
      Map<TrackId, TrackDataBuilder> trackDataBuilders = new HashMap<>();
      for (Ping ping : interpretationSettings.getPingSampler().getAvailablePings()) {
         List<TrackBorder> trackBorders = trackBorderExtractor.getTrackBorders(ping, channel)
               .toList();
         if (trackBorders.isEmpty()) {
            continue;
         }

         PingIndex pingIndex = ping.getPingIndex();

         EchogramPingSettings pingSettings = echogramModule.getPingSettings();
         float xMin = pingSettings.pingIndexToX(pingIndex);
         PingIndex nextPingIndex = dataFileSet.nextOrSame(pingIndex);
         float xMax = Math.max(xMin + 1, pingSettings.pingIndexToX(nextPingIndex));

         for (TrackBorder trackBorder : trackBorders) {
            float y = echogramModule.getZSettings().depthToY(trackBorder.peakDepth(), pingIndex);
            float yMin = echogramModule.getZSettings().depthToY(trackBorder.depthRange().min(), pingIndex);
            float yMax = echogramModule.getZSettings().depthToY(trackBorder.depthRange().max(), pingIndex);
            TrackId trackId = trackBorder.trackId();
            TrackDataBuilder trackDataBuilder = trackDataBuilders.get(trackId);
            if (trackDataBuilder == null) {
               trackDataBuilder = new TrackDataBuilder();
               trackDataBuilders.put(trackId, trackDataBuilder);
            }
            trackDataBuilder.add(new RangePoint(xMin, xMax, yMin, yMax), y, trackBorder.useAngles());
         }
      }

      Map<TrackId, TrackData> result = HashMap.newHashMap(trackDataBuilders.size());
      trackDataBuilders.forEach((trackId, trackDataBuilder) -> {
         result.put(trackId, trackDataBuilder.build(trackId, trackInfoModule));
      });
      return result;
   }

   @FunctionalInterface
   interface TrackBorderExtractor {
      Stream<TrackBorder> getTrackBorders(Ping ping, int channel);
   }

   record TrackData(
         TrackId trackId,
         Path2D.Float center,
         Path2D.Float extent,
         Drawable labelText,
         Path2D.Float ignoreAnglesCenter) {
   }

   private static final class TrackDataBuilder {
      private static final Path2D.Float EMPTY_PATH = new Path2D.Float(Path2D.WIND_NON_ZERO, 0);

      private final Path2D.Float center = new Path2D.Float();
      private final LineStripBuilder centerBuilder = LineStripBuilders.coalescing(center);
      private final List<RangePoint> rangePoints = new ArrayList<>();

      private Path2D.@Nullable Float ignoreAnglesCenter;
      private @Nullable LineStripBuilder ignoreAnglesCenterBuilder;

      private TrackDataBuilder() {
      }

      private void add(RangePoint rangePoint, float peakY, boolean useAngles) {
         centerBuilder.addPoint(rangePoint.xMin, peakY);
         centerBuilder.addPoint(rangePoint.xMax, peakY);
         rangePoints.add(rangePoint);

         if (useAngles) {
            if (ignoreAnglesCenterBuilder != null) {
               ignoreAnglesCenterBuilder.endLineStrip();
               ignoreAnglesCenterBuilder = null;
            }
         } else {
            if (ignoreAnglesCenterBuilder == null) {
               if (ignoreAnglesCenter == null) {
                  ignoreAnglesCenter = new Path2D.Float();
               }
               ignoreAnglesCenterBuilder = LineStripBuilders.coalescing(ignoreAnglesCenter);
            }
            ignoreAnglesCenterBuilder.addPoint(rangePoint.xMin, peakY);
            ignoreAnglesCenterBuilder.addPoint(rangePoint.xMax, peakY);
         }
      }

      private TrackData build(TrackId trackId, TrackInfoModule trackInfoModule) {
         Path2D.Float extent = new Path2D.Float();
         LineStripBuilder extentBuilder = LineStripBuilders.coalescing(extent);
         for (RangePoint p : rangePoints) {
            extentBuilder.addPoint(p.xMin, p.yMax);
            extentBuilder.addPoint(p.xMax, p.yMax);
         }
         for (int i = rangePoints.size() - 1; i >= 0; i--) {
            RangePoint p = rangePoints.get(i);
            extentBuilder.addPoint(p.xMax, p.yMin);
            extentBuilder.addPoint(p.xMin, p.yMin);
         }
         extentBuilder.endLineStrip();
         extent.closePath();
         centerBuilder.endLineStrip();
         if (ignoreAnglesCenterBuilder != null) {
            ignoreAnglesCenterBuilder.endLineStrip();
         }

         Drawable labelText;
         ImmutableSet<String> labels = trackInfoModule.getTrackLabelling().getLabels(trackId);
         if (labels.isEmpty()) {
            labelText = Drawable.nothing();
         } else {
            RangePoint centerRangePoint = rangePoints.get(rangePoints.size() / 2);
            float x = (centerRangePoint.xMin + centerRangePoint.xMax) / 2;
            float y = (centerRangePoint.yMin + centerRangePoint.yMax) / 2;
            labelText = new GuiText(Joiner.on(", ").join(labels), Color.BLACK, x, y,
                  GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.CENTER, null);
         }

         return new TrackData(trackId, center, extent, labelText, ignoreAnglesCenter != null ? ignoreAnglesCenter : EMPTY_PATH);
      }
   }

   private record RangePoint(float xMin, float xMax, float yMin, float yMax) {
   }
}
