package no.imr.korona.data.track;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.formats.missing.MissingBot0Datagram;
import no.imr.korona.data.formats.missing.MissingSegmentHandle;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingIndexShift;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.misc.ErrorHandler;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.RangeMap;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A track of echosounder data consisting of several contiguous {@link Segment}s.
 */
public class Track implements PingContainer {
   private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
   private final List<Segment> segments = new ArrayList<>();
   private final Map<PingMapping, RangeMap<Double, Segment>> pingMappingRangeMaps = new EnumMap<>(PingMapping.class);

   private PingConfiguration pingConfiguration = PingConfiguration.newEmpty();
   private PingRange totalPingRange = PingRange.EMPTY_RANGE;

   private ErrorHandler errorHandler = ErrorHandler.logging();

   public Track() {
      for (PingMapping pingMapping : PingMapping.values()) {
         pingMappingRangeMaps.put(pingMapping, new ArrayRangeMap<>());
      }
   }

   public ErrorHandler getErrorHandler() {
      return errorHandler;
   }

   public void setErrorHandler(ErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
   }

   public @Nullable Segment getFirstSegment() {
      Lock lock = readWriteLock.readLock();
      lock.lock();
      try {
         return segments.isEmpty() ? null : segments.getFirst();
      } finally {
         lock.unlock();
      }
   }

   public @Nullable Segment getLastSegment() {
      Lock lock = readWriteLock.readLock();
      lock.lock();
      try {
         return segments.isEmpty() ? null : segments.getLast();
      } finally {
         lock.unlock();
      }
   }

   public void add(Segment segment) {
      Lock lock = readWriteLock.writeLock();
      lock.lock();
      try {
         if (!segments.isEmpty()) {
            Segment previousSegment = segments.getLast();
            PingIndex newBegin = segment.getPingRange().begin();
            PingIndex previousEnd = previousSegment.getPingRange().end();

            long pingNumberShift = previousEnd.getPingNumber() - newBegin.getPingNumber();
            double vesselDistanceShift = previousEnd.getVesselDistance() - newBegin.getVesselDistance();
            segment.setPingIndexShift(PingIndexShift.create(Math.max(0, pingNumberShift), Math.max(0, vesselDistanceShift)));

            if (pingNumberShift < 0) {
               PingRange missingPingRange = PingRange.of(previousEnd, newBegin);
               MissingSegmentHandle missingSegmentHandle = new MissingSegmentHandle(previousSegment.getSegmentData().getPingConfiguration(), missingPingRange);
               internalAddSegment(new Segment(missingSegmentHandle, missingPingRange));
            }
         }

         internalAddSegment(segment);
      } finally {
         lock.unlock();
      }
   }

   public void removeFirstSegment() {
      Lock lock = readWriteLock.writeLock();
      lock.lock();
      try {
         if (segments.isEmpty()) {
            return;
         }

         internalRemoveFirstSegment();

         if (!segments.isEmpty() && segments.getFirst().getSegmentHandle() instanceof MissingSegmentHandle) {
            // Also remove possibly automatically inserted missing segment
            internalRemoveFirstSegment();
         }
      } finally {
         lock.unlock();
      }
   }

   private void internalAddSegment(Segment segment) {
      segment.setErrorHandler(errorHandler);
      if (!segments.isEmpty()) {
         Segment lastSegment = segments.getLast();
         updateRangeMaps(lastSegment.getPingRange().begin(), segment.getPingRange().begin(), lastSegment);
      }
      segments.add(segment);
      updateRangeMaps(segment.getPingRange().begin(), segment.getPingRange().end(), segment);
      updatePingRange();
   }

   private void internalRemoveFirstSegment() {
      Segment segment = segments.removeFirst();
      segment.close();

      PingIndex begin = segment.getPingRange().begin();
      PingIndex end = segments.size() > 1 ? segments.get(1).getPingRange().begin() : segment.getPingRange().end();
      updateRangeMaps(begin, end, null);
      updatePingRange();
   }

   public void updateLastSegmentRange() {
      Lock lock = readWriteLock.writeLock();
      lock.lock();
      try {
         if (segments.isEmpty()) {
            totalPingRange = PingRange.EMPTY_RANGE;
         } else {
            Segment segment = segments.getLast();
            updateRangeMaps(segment.getPingRange().begin(), segment.getPingRange().end(), segment);
            updatePingRange();
         }
      } finally {
         lock.unlock();
      }
   }

   private void updatePingRange() {
      if (segments.isEmpty()) {
         pingConfiguration = PingConfiguration.newEmpty();
         totalPingRange = PingRange.EMPTY_RANGE;
      } else {
         pingConfiguration = segments.getFirst().getSegmentData().getPingConfiguration();
         totalPingRange = PingRange.of(segments.getFirst().getPingRange().begin(), segments.getLast().getPingRange().end());
      }
   }

   private void updateRangeMaps(PingIndex firstIdx, PingIndex lastIdx, @Nullable Segment segment) {
      pingMappingRangeMaps.forEach((pingMapping, rangeMap) -> {
         rangeMap.put(pingMapping.valueOf(firstIdx), pingMapping.valueOf(lastIdx), segment);
      });
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public PingRange getTotalRange() {
      return totalPingRange;
   }

   public @Nullable Segment getSegment(double value, PingMapping pingMapping) {
      Lock lock = readWriteLock.readLock();
      lock.lock();
      try {
         Segment segment = pingMappingRangeMaps.get(pingMapping).get(value);
         if (segment == null && !segments.isEmpty() && segments.getLast().getPingRange().contains(value, pingMapping)) {
            // Last segment has expanded and this track has not been updated yet, so update now...
            lock.unlock(); // Must unlock read lock since write lock will be acquired
            try {
               updateLastSegmentRange();
            } finally {
               lock.lock();
            }
            segment = pingMappingRangeMaps.get(pingMapping).get(value);
         }
         return segment;
      } finally {
         lock.unlock();
      }
   }

   public @Nullable Segment getSegment(PingIndex pingIndex) {
      return getSegment(pingIndex.getPingNumber(), PingMapping.NUMBER);
   }

   public List<Segment> getSegments(PingRange pingRange) {
      Lock lock = readWriteLock.readLock();
      lock.lock();
      try {
         // Don't use sublist because of possible concurrency conflicts
         List<Segment> result = new ArrayList<>();
         for (Segment segment : segments) {
            if (pingRange.intersects(segment.getPingRange())) {
               result.add(segment);
            }
         }
         return result;
      } finally {
         lock.unlock();
      }
   }

   public int getSegmentCount() {
      return segments.size();
   }

   public List<Segment> getSegments() {
      return getSegments(totalPingRange);
   }

   @Override
   public PingIndex getClosestPingIndex(double value, PingMapping pingMapping) {
      Segment segment = getSegment(value, pingMapping);
      if (segment == null) {
         if (value < pingMapping.valueOf(totalPingRange.begin())) {
            return totalPingRange.begin();
         }
         return totalPingRange.end();
      }
      SegmentData segmentData = segment.getSegmentData();
      return DataUtils.getClosestPingIndex(segmentData.getPingIndices(), value, pingMapping);
   }

   @Override
   public @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping) {
      Segment segment = getSegment(value, pingMapping);
      if (segment == null) {
         return null;
      }
      SegmentData segmentData = segment.getSegmentData();
      return DataUtils.getContainingPingIndex(segmentData.getPingIndices(), totalPingRange.end(), value, pingMapping);
   }

   public Ping getPing(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      Segment segment = getSegment(pingIndex);
      if (segment == null) {
         RawFileConfiguration rawFileConfiguration = new RawFileConfiguration(0);
         return new DefaultPing(new PingConfiguration(rawFileConfiguration), pingIndex, new MissingBot0Datagram(rawFileConfiguration, pingIndex));
      }
      SegmentData segmentData = segment.getSegmentData();
      return segmentData.loadPing(pingIndex, asyncHandle);
   }

   public @Nullable Ping getPing(double value, PingMapping pingMapping, AsyncHandle asyncHandle) throws IOException {
      PingIndex pingIndex = getContainingPingIndex(value, pingMapping);
      if (pingIndex == null) {
         return null;
      }
      return getPing(pingIndex, asyncHandle);
   }
}
