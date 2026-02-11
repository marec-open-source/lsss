package no.imr.lsss.modules.korona.tracking;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import no.imr.korona.computation.tracking.TrackIdGenerator;
import no.imr.korona.data.datagrams.TBR0Datagram;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingRangeBuilder;
import no.imr.lsss.framework.WorkFileExtra;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TrackEditing {
   private final WorkFileExtra workFileExtra = new TrackEditingWorkFileExtra();
   private final TrackInfoModule trackInfoModule;
   private DataFileSet dataFileSet;
   private long firstPingNumber;
   private List<@Nullable List<TrackBorder>> edits = List.of();
   private final Set<TrackId> replacedTracks = ConcurrentHashMap.newKeySet();
   private final Map<TrackId, TrackInfo> newTracks = new ConcurrentHashMap<>();
   private final UndoManager undoManager = new UndoManager();

   TrackEditing(TrackInfoModule trackInfoModule) {
      this.trackInfoModule = trackInfoModule;
      dataFileSet = trackInfoModule.getLSSS().getInterpretationSettings().getDataFileSet();
      undoManager.setLimit(10);
   }

   WorkFileExtra getWorkFileExtra() {
      return workFileExtra;
   }

   void setDataFileSet(DataFileSet dataFileSet) {
      this.dataFileSet = dataFileSet;
   }

   public UndoManager getUndoManager() {
      return undoManager;
   }

   Set<TrackId> getReplacedTracks() {
      return replacedTracks;
   }

   Map<TrackId, TrackInfo> getNewTracks() {
      return newTracks;
   }

   public Stream<TrackBorder> getTrackBorders(Ping ping) {
      return internalGetTrackBorders(ping, _ -> true);
   }

   public Stream<TrackBorder> getTrackBorders(Ping ping, int selectedChannel) {
      return internalGetTrackBorders(ping, channel -> channel == selectedChannel);
   }

   private Stream<TrackBorder> internalGetTrackBorders(Ping ping, IntPredicate channelPredicate) {
      Stream<TrackBorder> originalStream = ping.getPingItems(TBR0Datagram.class)
            .filter(tbr0Datagram -> channelPredicate.test(tbr0Datagram.getChannel()))
            .map(tbr0Datagram -> {
               TrackId trackId = new TrackId(ping, tbr0Datagram.getId());
               if (replacedTracks.contains(trackId)) {
                  return null;
               }
               return new TrackBorder(trackId, tbr0Datagram.getChannel(), tbr0Datagram.getDepthRange(), tbr0Datagram.getPeakDepth(), true);
            })
            .filter(Objects::nonNull);
      int index = toIndex(ping.getPingNumber());
      if (index < 0 || index >= edits.size()) {
         return originalStream;
      }
      List<TrackBorder> trackBorders = edits.get(index);
      if (trackBorders == null) {
         return originalStream;
      }
      Stream<TrackBorder> editedStream = trackBorders.stream()
            .filter(trackBorder -> channelPredicate.test(trackBorder.channel()));
      return Stream.concat(originalStream, editedStream);
   }

   private List<TrackBorder> getOrCreateTrackBorders(PingIndex pingIndex) {
      int index = toIndex(pingIndex.getPingNumber());
      List<TrackBorder> trackBorders = edits.get(index);
      if (trackBorders == null) {
         trackBorders = new CopyOnWriteArrayList<>();
         edits.set(index, trackBorders);
      }
      return trackBorders;
   }

   private int toIndex(long pingNumber) {
      return (int) (pingNumber - firstPingNumber);
   }

   public TrackId newTrack(NavigableMap<PingIndex, TrackBorder> track) {
      TrackId trackId = newTrackId(track.firstKey());

      PingRange pingRange = PingRange.from(track, dataFileSet);

      ImmutableMap.Builder<PingIndex, TrackBorder> builder = ImmutableMap.builder();
      track.forEach((pingIndex, trackBorder) -> builder.put(pingIndex, trackBorder.withId(trackId)));

      TrackAddition trackAddition = new TrackAddition(new TrackInfo(trackId, null, pingRange), builder.build(), ImmutableSet.of());

      doEdits(ImmutableList.of(trackAddition));

      return trackId;
   }

   public void delete(Set<TrackId> trackIds) {
      ImmutableList<TrackRemoval> trackRemovals = trackIds.stream()
            .map(this::makeTrackRemoval)
            .collect(ImmutableList.toImmutableList());

      doEdits(trackRemovals);
   }

   public List<String> combine(Set<TrackId> trackIds, boolean join) {
      if (trackIds.size() < 2) {
         return List.of();
      }

      RangeSet<PingIndex> rangeSet = new ArrayRangeSet<>();
      PingRange pingRange = PingRange.EMPTY_RANGE;
      for (TrackId trackId : trackIds) {
         PingRange trackPingRange = trackInfoModule.getTrackInfos().get(trackId).pingRange();
         if (rangeSet.containsAny(trackPingRange)) {
            return List.of("Combining overlapping tracks is not supported");
         }
         rangeSet.add(trackPingRange);
         pingRange = pingRange.union(trackPingRange);
      }

      TrackLabelling trackLabelling = trackInfoModule.getTrackLabelling();
      ImmutableSet<String> labels = trackIds.stream()
            .map(trackLabelling::getLabels)
            .flatMap(Set::stream)
            .collect(ImmutableSet.toImmutableSet());

      TrackId newTrackId = newTrackId(pingRange.begin());
      ImmutableList.Builder<TrackEdit> trackEdits = ImmutableList.builder();
      TrackAddition trackAddition = makeTrackAddition(trackIds, pingRange, newTrackId, labels);
      if (join) {
         RangeSet<PingIndex> holesRangeSet = new ArrayRangeSet<>();
         holesRangeSet.add(pingRange);
         for (TrackId trackId : trackIds) {
            holesRangeSet.remove(trackInfoModule.getTrackInfos().get(trackId).pingRange());
         }
         TrackEditingDetection trackEditingDetection = new TrackEditingDetection(trackInfoModule, trackIds);
         for (Range<PingIndex> holeRange : holesRangeSet) {
            trackEditingDetection.fillHole(holeRange);
         }
         trackAddition = new TrackAddition(trackAddition.trackInfo, trackEditingDetection.getTrackBorders(newTrackId), trackAddition.labels);
      }
      trackEdits.add(trackAddition);
      trackIds.stream()
            .map(this::makeTrackRemoval)
            .forEach(trackEdits::add);

      doEdits(trackEdits.build());

      trackInfoModule.getTrackSelection().add(List.of(newTrackId));

      return List.of();
   }

   public List<TrackId> split(TrackId trackId, PingIndex pingIndex) {
      PingRange pingRange = trackInfoModule.getTrackInfos().get(trackId).pingRange();
      if (!pingRange.containsExcludingBegin(pingIndex)) {
         return ImmutableList.of();
      }

      TrackLabelling trackLabelling = trackInfoModule.getTrackLabelling();
      ImmutableSet<String> labels = trackLabelling.getLabels(trackId);
      boolean selected = trackInfoModule.getTrackSelection().getSelectedTrackIds().contains(trackId);

      TrackId newTrackIdA = newTrackId(pingRange.begin());
      TrackAddition trackAdditionA = makeTrackAddition(Set.of(trackId), PingRange.of(pingRange.begin(), pingIndex), newTrackIdA, labels);

      TrackId newTrackIdB = newTrackId(pingIndex);
      TrackAddition trackAdditionB = makeTrackAddition(Set.of(trackId), PingRange.of(pingIndex, pingRange.end()), newTrackIdB, labels);

      TrackRemoval trackRemoval = makeTrackRemoval(trackId);

      doEdits(ImmutableList.of(trackAdditionA, trackAdditionB, trackRemoval));

      if (selected) {
         trackInfoModule.getTrackSelection().add(List.of(newTrackIdA, newTrackIdB));
      }

      return ImmutableList.of(newTrackIdA, newTrackIdB);
   }

   public void extend(TrackId trackId, boolean left, float minTSU) {
      TrackEditingDetection trackEditingDetection = new TrackEditingDetection(trackInfoModule, Set.of(trackId));
      while (true) {
         TrackBorder trackBorder = trackEditingDetection.extend(left, minTSU);
         if (trackBorder == null) {
            break;
         }
      }
      apply(trackEditingDetection);
   }

   @Nullable TrackId apply(TrackEditingDetection trackEditingDetection) {
      if (!trackEditingDetection.didEdit()) {
         return null;
      }

      if (trackEditingDetection.getTrackBorders().isEmpty()) {
         delete(trackEditingDetection.getTrackIds());
         return null;
      }

      TrackLabelling trackLabelling = trackInfoModule.getTrackLabelling();
      ImmutableSet<String> labels = trackEditingDetection.getTrackIds().stream()
            .map(trackLabelling::getLabels)
            .flatMap(Set::stream)
            .collect(ImmutableSet.toImmutableSet());

      TrackId newTrackId = newTrackId(trackEditingDetection.getPingRange().begin());

      ImmutableList.Builder<TrackEdit> trackEdits = ImmutableList.builder();
      TrackInfo trackInfo = new TrackInfo(newTrackId, null, trackEditingDetection.getPingRange());
      TrackAddition trackAddition = new TrackAddition(trackInfo, trackEditingDetection.getTrackBorders(newTrackId), labels);
      trackEdits.add(trackAddition);
      trackEditingDetection.getTrackIds().stream()
            .map(this::makeTrackRemoval)
            .forEach(trackEdits::add);

      doEdits(trackEdits.build());

      trackInfoModule.getTrackSelection().add(List.of(newTrackId));

      return newTrackId;
   }

   private TrackAddition makeTrackAddition(Set<TrackId> trackIds, PingRange pingRange, TrackId newTrackId, ImmutableSet<String> labels) {
      ImmutableMap.Builder<PingIndex, TrackBorder> trackBordersBuilder = ImmutableMap.builder();
      dataFileSet.getPingIndices(pingRange).forEach(pingIndex -> {
         Ping ping = dataFileSet.getPing(pingIndex);
         getTrackBorders(ping)
               .filter(trackBorder -> trackIds.contains(trackBorder.trackId()))
               .map(trackBorder -> trackBorder.withId(newTrackId))
               .forEach(trackBorder -> trackBordersBuilder.put(pingIndex, trackBorder));
      });
      ImmutableMap<PingIndex, TrackBorder> trackBorders = trackBordersBuilder.build();

      TrackInfo trackInfo = new TrackInfo(newTrackId, null, PingRange.from(trackBorders.keySet(), dataFileSet));
      newTracks.put(newTrackId, trackInfo);

      return new TrackAddition(trackInfo, trackBorders, labels);
   }

   private TrackRemoval makeTrackRemoval(TrackId trackId) {
      if (newTracks.containsKey(trackId)) {
         TrackInfo removedTrackInfo = newTracks.remove(trackId);
         ImmutableMap.Builder<PingIndex, TrackBorder> trackBordersBuilder = ImmutableMap.builder();
         dataFileSet.getPingIndices(removedTrackInfo.pingRange()).forEach(pingIndex -> {
            List<TrackBorder> trackBorders = edits.get(toIndex(pingIndex.getPingNumber()));
            if (trackBorders != null) {
               for (TrackBorder trackBorder : trackBorders) {
                  if (trackBorder.trackId().equals(trackId)) {
                     trackBordersBuilder.put(pingIndex, trackBorder);
                  }
               }
            }
         });
         return new EditedTrackRemoval(removedTrackInfo, trackBordersBuilder.build());
      } else {
         return new OriginalTrackRemoval(trackId);
      }
   }

   private void doEdits(ImmutableList<? extends TrackEdit> trackEdits) {
      trackEdits.forEach(TrackEdit::doIt);
      trackInfoModule.updateFromEditing();

      undoManager.addEdit(new AbstractUndoableEdit() {
         @Override
         public void undo() {
            super.undo();

            trackEdits.forEach(TrackEdit::undoIt);
            trackInfoModule.updateFromEditing();
         }

         @Override
         public void redo() {
            super.redo();

            trackEdits.forEach(TrackEdit::doIt);
            trackInfoModule.updateFromEditing();
         }
      });
   }

   private TrackId newTrackId(PingIndex pingIndex) {
      DataFile dataFile = dataFileSet.getDataFile(pingIndex);
      int channel = trackInfoModule.getLSSS().getInterpretationSettings().getChannel();
      TrackIdGenerator trackIdGenerator = new TrackIdGenerator(channel, dataFile.getPingRange().begin(), dataFile.getRawFileConfiguration());
      long pingNumber = dataFile.getPingRange().end().getPingNumber(); // One past actual ping indices => not used for original tracks
      return IntStream.range(0, Integer.MAX_VALUE)
            .mapToObj(sampleIndex -> new TrackId(dataFile, trackIdGenerator.nextId(pingNumber, sampleIndex)))
            .filter(Predicate.not(newTracks::containsKey))
            .findAny()
            .orElseThrow(() -> new ShouldNotHappenException("Could not find available track id"));
   }

   private void reset() {
      PingRange totalRange = dataFileSet.getTotalRange();
      firstPingNumber = totalRange.begin().getPingNumber();
      int n = totalRange.getPingCount();
      edits = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
         edits.add(null);
      }
      newTracks.clear();
      replacedTracks.clear();
      undoManager.discardAllEdits();
   }

   private abstract static class TrackEdit {
      private TrackEdit() {
      }

      abstract void doIt();

      abstract void undoIt();
   }

   private final class TrackAddition extends TrackEdit {
      private final TrackInfo trackInfo;
      private final ImmutableMap<PingIndex, TrackBorder> trackBorders;
      private final ImmutableSet<String> labels;

      private TrackAddition(TrackInfo trackInfo, ImmutableMap<PingIndex, TrackBorder> trackBorders, ImmutableSet<String> labels) {
         this.trackInfo = trackInfo;
         this.trackBorders = trackBorders;
         this.labels = labels;
      }

      @Override
      void doIt() {
         newTracks.put(trackInfo.trackId(), trackInfo);
         trackBorders.forEach((pingIndex, trackBorder) -> {
            getOrCreateTrackBorders(pingIndex).add(trackBorder);
         });
         trackInfoModule.getTrackLabelling().setLabels(trackInfo.trackId(), labels);
      }

      @Override
      void undoIt() {
         newTracks.remove(trackInfo.trackId());
         trackBorders.forEach((pingIndex, trackBorder) -> {
            getOrCreateTrackBorders(pingIndex).remove(trackBorder);
         });
      }
   }

   private abstract static class TrackRemoval extends TrackEdit {
      private TrackRemoval() {
      }
   }

   private final class OriginalTrackRemoval extends TrackRemoval {
      private final TrackId trackId;
      private final ImmutableSet<String> labels;

      private OriginalTrackRemoval(TrackId trackId) {
         this.trackId = trackId;
         labels = trackInfoModule.getTrackLabelling().getLabels(trackId);
      }

      @Override
      void doIt() {
         replacedTracks.add(trackId);
      }

      @Override
      void undoIt() {
         replacedTracks.remove(trackId);
         trackInfoModule.getTrackLabelling().setLabels(trackId, labels);
      }
   }

   private final class EditedTrackRemoval extends TrackRemoval {
      private final TrackAddition trackAddition;

      private EditedTrackRemoval(TrackInfo trackInfo, ImmutableMap<PingIndex, TrackBorder> trackBorders) {
         trackAddition = new TrackAddition(trackInfo, trackBorders, trackInfoModule.getTrackLabelling().getLabels(trackInfo.trackId()));
      }

      @Override
      void doIt() {
         trackAddition.undoIt();
      }

      @Override
      void undoIt() {
         trackAddition.doIt();
      }
   }

   private final class TrackEditingWorkFileExtra extends WorkFileExtra {
      private static final String XML_REPLACED_IDS = "replacedIds";
      private static final String XML_TRACKS = "tracks";
      private static final String XML_TRACK = "track";
      private static final String XML_ID = "id";
      private static final String XML_CHANNEL = "channel";
      private static final String XML_PING = "ping";
      private static final String XML_NT_DATE = "ntDate";
      private static final String XML_MIN_DEPTH = "minDepth";
      private static final String XML_MAX_DEPTH = "maxDepth";
      private static final String XML_PEAK_DEPTH = "peakDepth";
      private static final String XML_IGNORE_ANGLES = "ignoreAngles";

      private TrackEditingWorkFileExtra() {
         super("trackEdits");
      }

      @Override
      public void toXml(DataFile dataFile, Element element) {
         int[] discardedIds = replacedTracks.stream()
               .filter(trackId -> trackId.rawFileConfigurationNTDate() == dataFile.getRawFileConfiguration().getNTDate())
               .mapToInt(TrackId::id)
               .toArray();
         if (discardedIds.length == 0) {
            return;
         }
         Arrays.sort(discardedIds);
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < discardedIds.length; i++) {
            if (i > 0) {
               sb.append(i % 10 == 0 ? '\n' : ' ');
            }
            sb.append(discardedIds[i]);
         }
         element.addElement(XML_REPLACED_IDS)
               .addText(sb.toString());

         Element tracksElement = DocumentHelper.createElement(XML_TRACKS);
         Map<TrackId, Element> idToTrackElement = new HashMap<>();
         dataFile.getPingIndices().forEach(pingIndex -> {
            List<TrackBorder> trackBorders = edits.get(toIndex(pingIndex.getPingNumber()));
            if (trackBorders == null || trackBorders.isEmpty()) {
               return;
            }
            trackBorders.forEach(trackBorder -> {
               Element trackElement = idToTrackElement.computeIfAbsent(trackBorder.trackId(), key -> {
                  return tracksElement.addElement(XML_TRACK)
                        .addAttribute(XML_ID, key.toIdString(dataFile.getRawFileConfiguration()))
                        .addAttribute(XML_CHANNEL, Integer.toString(trackBorder.channel()));
               });
               Element pingElement = trackElement.addElement(XML_PING)
                     .addAttribute(XML_NT_DATE, Long.toString(pingIndex.getNTDate()))
                     .addAttribute(XML_MIN_DEPTH, Float.toString(trackBorder.depthRange().min()))
                     .addAttribute(XML_MAX_DEPTH, Float.toString(trackBorder.depthRange().max()))
                     .addAttribute(XML_PEAK_DEPTH, Float.toString(trackBorder.peakDepth()));
               if (!trackBorder.useAngles()) {
                  pingElement.addAttribute(XML_IGNORE_ANGLES, "true");
               }
            });
         });
         if (!idToTrackElement.isEmpty()) {
            element.add(tracksElement);
         }
      }

      @Override
      public void beginFromXml() {
         setDataFileSet(trackInfoModule.getLSSS().getInterpretationSettings().getDataFileSet());
         reset();
      }

      @Override
      public void fromXml(DataFile dataFile, @Nullable Element element, int originalVersion) {
         if (element == null) {
            return;
         }
         Element editedIdsElement = element.element(XML_REPLACED_IDS);
         if (editedIdsElement == null) {
            return;
         }
         for (String word : editedIdsElement.getText().split("\\s")) {
            int id = Integer.parseInt(word);
            replacedTracks.add(new TrackId(dataFile, id));
         }
         Element tracksElement = element.element(XML_TRACKS);
         if (tracksElement != null) {
            tracksElement.elements().forEach(trackElement -> {
               TrackId trackId = TrackId.fromIdString(dataFile.getRawFileConfiguration(), trackElement.attributeValue(XML_ID));
               int channel = Integer.parseInt(trackElement.attributeValue(XML_CHANNEL));
               PingRangeBuilder pingRangeBuilder = new PingRangeBuilder();
               for (Element pingElement : trackElement.elements()) {
                  long ntDate = Long.parseLong(pingElement.attributeValue(XML_NT_DATE));
                  float minDepth = Float.parseFloat(pingElement.attributeValue(XML_MIN_DEPTH));
                  float maxDepth = Float.parseFloat(pingElement.attributeValue(XML_MAX_DEPTH));
                  float peakDepth = Float.parseFloat(pingElement.attributeValue(XML_PEAK_DEPTH));
                  boolean useAngles = !Boolean.parseBoolean(pingElement.attributeValue(XML_IGNORE_ANGLES));
                  TrackBorder trackBorder = new TrackBorder(trackId, channel, FloatRange.of(minDepth, maxDepth), peakDepth, useAngles);
                  PingIndex pingIndex = dataFileSet.getContainingPingIndex(PingMapping.ntDateToTimeValue(ntDate), PingMapping.TIME);
                  if (pingIndex == null) {
                     continue;
                  }
                  getOrCreateTrackBorders(pingIndex).add(trackBorder);
                  pingRangeBuilder.add(pingIndex);
               }
               PingRange pingRange = pingRangeBuilder.build(dataFileSet);
               if (pingRange.isEmpty()) {
                  return;
               }
               TrackInfo trackInfo = new TrackInfo(trackId, null, pingRange);
               TrackInfo previousTrackInfo = newTracks.put(trackId, trackInfo);
               if (previousTrackInfo != null) {
                  newTracks.put(trackId, new TrackInfo(trackId, null, trackInfo.pingRange().union(previousTrackInfo.pingRange())));
               }
            });
         }
      }

      @Override
      public void endFromXml() {
         trackInfoModule.updateFromEditing();
      }
   }
}
