package no.imr.lsss.modules.korona.tracking;

import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.region.EchogramSelection;
import no.imr.tools.Utils;
import no.imr.tools.listening.ArgChangeManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class TrackSelection {
   private final TrackInfoModule trackInfoModule;
   private final Set<TrackId> selectedTrackIds = ConcurrentHashMap.newKeySet();
   private final ArgChangeManager<Set<TrackId>> changeManager = new ArgChangeManager<>();

   TrackSelection(TrackInfoModule trackInfoModule) {
      this.trackInfoModule = trackInfoModule;

      trackInfoModule.getValidIdsChangeManager().addListener(this::retain);
   }

   public Set<TrackId> getSelectedTrackIds() {
      return selectedTrackIds;
   }

   public ArgChangeManager<Set<TrackId>> getChangeManager() {
      return changeManager;
   }

   public void add(Collection<TrackId> trackIds) {
      selectedTrackIds.addAll(trackIds);
      changeManager.notifyListeners(selectedTrackIds);
   }

   public void remove(Collection<TrackId> trackIds) {
      selectedTrackIds.removeAll(trackIds);
      changeManager.notifyListeners(selectedTrackIds);
   }

   public void replace(Collection<TrackId> trackIds) {
      selectedTrackIds.clear();
      selectedTrackIds.addAll(trackIds);
      changeManager.notifyListeners(selectedTrackIds);
   }

   public void retain(Collection<TrackId> trackIds) {
      selectedTrackIds.retainAll(trackIds);
      changeManager.notifyListeners(selectedTrackIds);
   }

   public void selectByPredicate(Predicate<TrackId> predicate) {
      trackInfoModule.getValidIds().stream()
            .filter(predicate)
            .forEach(selectedTrackIds::add);
      changeManager.notifyListeners(selectedTrackIds);
   }

   public void deselectByPredicate(Predicate<TrackId> predicate) {
      selectedTrackIds.removeIf(predicate);
      changeManager.notifyListeners(selectedTrackIds);
   }

   public void toggle(Collection<TrackId> trackIds) {
      Utils.toggle(selectedTrackIds, trackIds);
      changeManager.notifyListeners(selectedTrackIds);
   }

   void doEchogramSelection(EchogramSelection echogramSelection) {
      Set<TrackId> trackIds = getTrackIds(echogramSelection.echogramRectangle());
      switch (echogramSelection.action()) {
         case ADD -> add(trackIds);
         case REPLACE -> replace(trackIds);
         case TOGGLE -> toggle(trackIds);
      }
   }

   private Set<TrackId> getTrackIds(EchogramRectangle echogramRectangle) {
      Set<TrackId> trackIds = new HashSet<>();
      Set<TrackId> validIds = trackInfoModule.getValidIds();
      int channel = trackInfoModule.getLSSS().getInterpretationSettings().getChannel();
      trackInfoModule.getLSSS().getInterpretationSettings().getPingSampler().getAvailablePings().forEach(ping -> {
         if (!echogramRectangle.pingRange().contains(ping)) {
            return;
         }
         trackInfoModule.getTrackEditing().getTrackBorders(ping, channel).forEach(trackBorder -> {
            TrackId trackId = trackBorder.trackId();
            if (validIds.contains(trackId) && echogramRectangle.depthRange(ping.getPingIndex()).intersects(trackBorder.depthRange())) {
               trackIds.add(trackId);
            }
         });
      });
      return trackIds;
   }
}
