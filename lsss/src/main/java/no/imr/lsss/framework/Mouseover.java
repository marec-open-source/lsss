package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.tools.listening.ListenableProperty;
import no.marec.lsss.api.util.GeoPoint;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class Mouseover {
   private final InterpretationSettings interpretationSettings;
   private final ListenableProperty<Boolean> mouseFrozen = new ListenableProperty<>(false);
   private final ListenableProperty<Optional<GeoPoint>> mouseGeoPos = new ListenableProperty<>(Optional.empty());
   private final ListenableProperty<Optional<EchogramPoint>> mouseEchogramPoint = new ListenableProperty<>(Optional.empty());
   private final ListenableProperty<Optional<PingIndex>> mousePingIndex = new ListenableProperty<>(Optional.empty());
   private final ListenableProperty<Optional<Float>> mouseDepth = new ListenableProperty<>(Optional.empty());
   private final ListenableProperty<Optional<Float>> mouseKHz = new ListenableProperty<>(Optional.empty());

   Mouseover(InterpretationSettings interpretationSettings) {
      this.interpretationSettings = interpretationSettings;
   }

   public ObservableValue<Boolean> frozen() {
      return mouseFrozen;
   }

   public boolean isFrozen() {
      return mouseFrozen.getValue();
   }

   public void setFrozen(boolean frozen) {
      mouseFrozen.setValue(frozen);
   }

   public ObservableValue<Optional<GeoPoint>> geoPos() {
      return mouseGeoPos;
   }

   public @Nullable GeoPoint getGeoPos() {
      return mouseGeoPos.getValue().orElse(null);
   }

   public ObservableValue<Optional<EchogramPoint>> echogramPoint() {
      return mouseEchogramPoint;
   }

   public @Nullable EchogramPoint getEchogramPoint() {
      return mouseEchogramPoint.getValue().orElse(null);
   }

   public ObservableValue<Optional<PingIndex>> pingIndex() {
      return mousePingIndex;
   }

   public @Nullable PingIndex getPingIndex() {
      return mousePingIndex.getValue().orElse(null);
   }

   public ObservableValue<Optional<Float>> depth() {
      return mouseDepth;
   }

   public @Nullable Float getDepth() {
      return mouseDepth.getValue().orElse(null);
   }

   public void setPos() {
      setPos(null, null, null);
   }

   public void setPos(@Nullable GeoPoint geoPos) {
      setPos(geoPos, null);
   }

   public void setPos(@Nullable GeoPoint geoPos, @Nullable Float depth) {
      DataFileSet dataFileSet = interpretationSettings.getDataFileSet();
      PingIndex pingIndex = geoPos != null ? DataUtils.geoPosToClosestPingIndex(geoPos, dataFileSet.getPingIndices()) : null;
      pingIndex = pingIndex != null ? DataUtils.geoPosToProjectedPingIndex(geoPos, pingIndex, dataFileSet) : null;
      if (pingIndex != null && pingIndex.equals(dataFileSet.getTotalRange().end())) {
         pingIndex = dataFileSet.previousOrNull(pingIndex);
      }
      setPos(geoPos, pingIndex, depth);
   }

   public void setPos(@Nullable EchogramPoint echogramPoint) {
      PingIndex pingIndex = echogramPoint != null ? echogramPoint.pingIndex() : null;
      Float depth = echogramPoint != null ? echogramPoint.depth() : null;
      setPos(pingIndex, depth);
   }

   public void setPos(@Nullable PingIndex pingIndex) {
      setPos(pingIndex, null);
   }

   public void setPos(@Nullable PingIndex pingIndex, @Nullable Float depth) {
      GeoPoint geoPos = pingIndex != null ? pingIndex.getGeographicalPosition() : null;
      setPos(geoPos, pingIndex, depth);
   }

   public void setPos(@Nullable GeoPoint geoPos, @Nullable PingIndex pingIndex, @Nullable Float depth) {
      if (isFrozen()) {
         return;
      }
      mouseGeoPos.setValue(Optional.ofNullable(geoPos));
      mouseEchogramPoint.setValue(Optional.ofNullable(pingIndex != null && depth != null ? new EchogramPoint(pingIndex, depth) : null));
      mousePingIndex.setValue(Optional.ofNullable(pingIndex));
      mouseDepth.setValue(Optional.ofNullable(depth));
   }

   public ObservableValue<Optional<Float>> kHz() {
      return mouseKHz;
   }

   public @Nullable Float getKHz() {
      return mouseKHz.getValue().orElse(null);
   }

   public void setKHz(@Nullable Float kHz) {
      if (isFrozen()) {
         return;
      }
      mouseKHz.setValue(Optional.ofNullable(kHz));
   }
}
