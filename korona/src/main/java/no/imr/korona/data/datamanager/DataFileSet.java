package no.imr.korona.data.datamanager;

import com.google.common.util.concurrent.AtomicDouble;
import no.imr.korona.data.DataException;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.formats.missing.MissingPingIndex;
import no.imr.korona.data.formats.missing.MissingSegmentHandle;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.Min;
import no.imr.tools.ProgressHandler;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeMap;
import no.imr.tools.range.RangeSet;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * An immutable set of {@link DataFile}s.
 */
public final class DataFileSet implements PingContainer {
   private final DataConfiguration dataConfiguration;
   private final Executor pingLoaderExecutor = new SerialExecutor(Exec.CACHED_THREAD_POOL);
   private final List<DataFile> dataFiles;
   private final RangeSet<PingIndex> missingPings = new ArrayRangeSet<>();
   private final RangeMap<Instant, Double> wrapAroundRangeMap = new ArrayRangeMap<>();
   private final PingConfiguration pingConfiguration;
   private final PingRange totalPingRange;
   private final List<PingIndex> pingIndices;
   private final List<Bot0Datagram> bot0Datagrams;
   private @Nullable Float maxDepth;
   private final List<DataManager> dataManagers = new CopyOnWriteArrayList<>();

   DataFileSet(DataConfiguration dataConfiguration, FileOpenRequest fileOpenRequest) {
      this.dataConfiguration = dataConfiguration;

      dataFiles = new ArrayList<>();
      openFiles(fileOpenRequest);

      if (dataFiles.isEmpty()) {
         pingConfiguration = PingConfiguration.newEmpty();
         totalPingRange = PingRange.EMPTY_RANGE;
         pingIndices = List.of();
         bot0Datagrams = List.of();
      } else {
         pingConfiguration = dataFiles.getFirst().getPingConfiguration();
         totalPingRange = toTotalPingRange(dataFiles);
         pingIndices = dataFiles.stream()
               .<PingIndex>flatMap(dataFile -> dataFile.getPingIndices().stream())
               .toList();
         bot0Datagrams = dataFiles.stream()
               .flatMap(dataFile -> dataFile.getBot0Datagrams().stream())
               .toList();
      }
   }

   private DataFileSet(DataFileSet dataFileSet, int beginIndex, int endIndex) {
      dataConfiguration = dataFileSet.dataConfiguration;
      dataFiles = dataFileSet.dataFiles.subList(beginIndex, endIndex);
      pingConfiguration = dataFileSet.pingConfiguration;
      totalPingRange = toTotalPingRange(dataFiles);
      int pingBeginIndex = dataFileSet.pingNumberToIndex(totalPingRange.begin().getPingNumber());
      int pingEndIndex = pingBeginIndex + totalPingRange.getPingCount();
      pingIndices = dataFileSet.pingIndices.subList(pingBeginIndex, pingEndIndex);
      bot0Datagrams = dataFileSet.bot0Datagrams.subList(pingBeginIndex, pingEndIndex);
      dataFileSet.missingPings.stream(totalPingRange).forEach(missingPings::add);
      wrapAroundRangeMap.putAll(dataFileSet.wrapAroundRangeMap);
   }

   public DataFileSet subDataFileSet(int beginIndex, int endIndex) {
      if (beginIndex >= endIndex) {
         return new DataFileSet(dataConfiguration, new FileOpenRequest(List.of()));
      }
      return new DataFileSet(this, beginIndex, endIndex);
   }

   private static PingRange toTotalPingRange(List<DataFile> dataFiles) {
      return PingRange.of(dataFiles.getFirst().getPingRange().begin(), dataFiles.getLast().getPingRange().end());
   }

   public static DataFileSet empty() {
      return new DataFileSet(new DefaultDataConfiguration(), new FileOpenRequest(List.of()));
   }

   @Override
   public String toString() {
      return totalPingRange.toString();
   }

   public DataConfiguration getDataConfiguration() {
      return dataConfiguration;
   }

   void addDataManager(DataManager dataManager) {
      dataManagers.add(dataManager);
   }

   void removeDataManager(DataManager dataManager) {
      dataManagers.remove(dataManager);
      if (dataManagers.isEmpty()) {
         close();
      }
   }

   void close() {
      dataFiles.forEach(DataFile::close);
   }

   public Compatibility getCompatibilityWith(DataFileSet otherDataFileSet) {
      if (dataFiles.size() != otherDataFileSet.getDataFiles().size()) {
         return Compatibility.UNUSABLE;
      }

      Compatibility compatibility = Compatibility.OK;
      for (int i = 0; i < dataFiles.size(); i++) {
         DataFile dataFile = dataFiles.get(i);
         DataFile otherDataFile = otherDataFileSet.getDataFiles().get(i);
         if (!dataFile.getPingRange().equals(otherDataFile.getPingRange())) {
            return Compatibility.UNUSABLE;
         }
         if (dataFile.getPingConfiguration().getIncompatibility(otherDataFile.getPingConfiguration()) != null) {
            compatibility = Compatibility.INCOMPATIBLE_PING_CONFIGURATION;
            // Continue checking all files in case some other files have different ping range
         }
      }

      return compatibility;
   }

   private void registerDataFile(DataFile dataFile) {
      dataFiles.add(dataFile);

      if (dataFile.getSegmentHandle() instanceof MissingSegmentHandle) {
         missingPings.add(dataFile.getPingRange());
      }

      addWrapAround(dataFile.getWrapAround());
   }

   private void addWrapAround(@Nullable WrapAround wrapAround) {
      if (wrapAround != null) {
         Instant key = wrapAround.pingIndex().getInstant();
         double value = getWrapAround(key) + wrapAround.vesselDistance();
         wrapAroundRangeMap.put(key, Instant.MAX, value);
      }
   }

   public double getVesselDistanceUncorrectedForWrapAround(PingIndex pingIndex) {
      double wrapAroundCorrection = getWrapAround(pingIndex.getInstant());
      return pingIndex.getVesselDistance() - wrapAroundCorrection;
   }

   private double getWrapAround(Instant key) {
      Double nullableValue = wrapAroundRangeMap.get(key);
      return nullableValue != null ? nullableValue : 0.0;
   }

   private void openFiles(FileOpenRequest fileOpenRequest) {
      List<SegmentHandle> segmentHandles = fileOpenRequest.getSegmentHandles();
      List<Future<DataFileResult>> futureResults = segmentHandles.stream()
            .map(segmentHandle -> {
               return Exec.LONG_RUNNING_THREAD_POOL.submit(() -> {
                  if (fileOpenRequest.getAsyncHandle().isCancelled()) {
                     return new DataFileResult(null, null);
                  }
                  try {
                     return new DataFileResult(new DataFile(this, segmentHandle, fileOpenRequest), null);
                  } catch (IOException e) {
                     return new DataFileResult(null, e);
                  }
               });
            })
            .toList();

      // If cancelled we must complete the loop to close any open files
      for (int i = 0; i < segmentHandles.size(); i++) {
         SegmentHandle segmentHandle = segmentHandles.get(i);
         DataFile dataFile = null;
         try {
            DataFileResult result = futureResults.get(i).get();
            dataFile = result.dataFile;
            IOException e = result.exception;
            if (dataFile != null) {
               if (fileOpenRequest.getAsyncHandle().isCancelled()) {
                  dataFile.close();
                  continue;
               } else {
                  dataFile = adjustAndRegisterDataFile(dataFile, fileOpenRequest);
               }
            } else if (e instanceof DataException dataException) {
               fileOpenRequest.getObserver().handleDataException(segmentHandle, dataException);
            } else if (e != null) {
               Log.global.log(Level.WARNING, "Error opening " + segmentHandle, e);
            } else {
               // Cancelled
               continue;
            }
         } catch (CancellationException _) {
            // Cancelled
            fileOpenRequest.getAsyncHandle().cancel();
            continue;
         } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            fileOpenRequest.getAsyncHandle().cancel();
            continue;
         } catch (ExecutionException e) {
            Log.global.log(Level.WARNING, "Error opening " + segmentHandle, e);
         }

         fileOpenRequest.getObserver().progressReportAfterOpen(segmentHandle, dataFile);
      }

      if (fileOpenRequest.getAsyncHandle().isCancelled()) {
         close();
      }
   }

   private @Nullable DataFile adjustAndRegisterDataFile(DataFile dataFile, FileOpenRequest fileOpenRequest) {
      if (!dataFiles.isEmpty()) { // Check data integrity
         DataFile previousDataFile = dataFiles.getLast();

         if (fileOpenRequest.getCheckCompatibility()) {
            String incompatibility = dataFile.getPingConfiguration().getIncompatibility(previousDataFile.getPingConfiguration());
            if (incompatibility != null) {
               fileOpenRequest.getObserver().handleIncompatibleDataFile(dataFile.getSegmentHandle(), incompatibility);
               dataFile.close();
               return null;
            }
         }

         PingIndex firstInNext = dataFile.getPingIndices().getFirst();
         PingIndex lastInPrevious = previousDataFile.getPingIndices().getLast();

         long nanosDiff = lastInPrevious.getInstant().until(firstInNext.getInstant(), ChronoUnit.NANOS);
         if (nanosDiff <= 0) {
            fileOpenRequest.getObserver().handleIncompatibleDataFile(dataFile.getSegmentHandle(),
                  "Incorrect time ordering by " + Utils.format("%.3g", -nanosDiff / 1e9) + " seconds");
            dataFile.close();
            return null;
         }

         double previousFileUncorrectedVesselDistance = getVesselDistanceUncorrectedForWrapAround(previousDataFile.getPingRange().end());
         if (WrapAround.isWrapAround(previousFileUncorrectedVesselDistance, dataFile.getPingRange().begin().getVesselDistance())) {
            addWrapAround(new WrapAround(dataFile.getPingRange().begin(), WrapAround.roundToPowerOfTen(previousFileUncorrectedVesselDistance)));
         }

         Double wrapAroundCorrection = wrapAroundRangeMap.get(firstInNext.getInstant());
         if (wrapAroundCorrection != null) {
            dataFile.shiftVesselDistance(wrapAroundCorrection, false);
         }

         // Vessel distance: test with last real PingIndex, but use extrapolated PingIndex in shift value.
         double vesselDistanceShift = lastInPrevious.getVesselDistance() - dataFile.getPingRange().begin().getVesselDistance();
         if (vesselDistanceShift > 0) {
            vesselDistanceShift = previousDataFile.getPingRange().end().getVesselDistance() - dataFile.getPingRange().begin().getVesselDistance();
            dataFile.shiftVesselDistance(vesselDistanceShift, true);
         }

         long pingNumberShift = previousDataFile.getPingRange().end().getPingNumber() - dataFile.getPingRange().begin().getPingNumber();
         if (pingNumberShift > 0) {
            dataFile.shiftPingNumber(pingNumberShift);
         } else if (pingNumberShift < 0) {
            double f;
            MissingPingIndex firstInMissing;
            List<? extends PingIndex> previousPingIndices = previousDataFile.getPingIndices();
            if (previousPingIndices.size() > 1) {
               // Interpolate from extrapolated time.
               long deltaNanos = previousPingIndices.get(previousPingIndices.size() - 2).getInstant().until(lastInPrevious.getInstant(), ChronoUnit.NANOS);
               Instant targetInstant = Min.of(lastInPrevious.getInstant().plusNanos(deltaNanos), firstInNext.getInstant().minusNanos(100));
               f = TimeUtils.toSeconds(lastInPrevious.getInstant(), targetInstant) / TimeUtils.toSeconds(lastInPrevious.getInstant(), firstInNext.getInstant());
            } else {
               // Interpolate from ping number.
               f = 1 / (double) (firstInNext.getPingNumber() - lastInPrevious.getPingNumber());
            }
            firstInMissing = MissingPingIndex.create(lastInPrevious, firstInNext, lastInPrevious.getPingNumber() + 1, f);
            PingRange missingPingRange = PingRange.of(firstInMissing, firstInNext);
            try {
               registerDataFile(new DataFile(this, new MissingSegmentHandle(previousDataFile.getPingConfiguration(), missingPingRange), fileOpenRequest));
            } catch (IOException e) {
               throw new ShouldNotHappenException(e);
            }
         }
      }

      registerDataFile(dataFile);

      return dataFile;
   }

   public List<DataFile> getDataFiles() {
      return dataFiles;
   }

   public boolean isEmpty() {
      return dataFiles.isEmpty();
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public PingRange getTotalRange() {
      return totalPingRange;
   }

   public RangeSet<PingIndex> getMissingPings() {
      return missingPings;
   }

   private int pingNumberToIndex(long pingNumber) {
      return (int) (pingNumber - totalPingRange.begin().getPingNumber());
   }

   @Override
   public @Nullable PingIndex getPingIndexOrNullExcludingEnd(long pingNumber) {
      int i = pingNumberToIndex(pingNumber);
      if (i >= 0 && i < pingIndices.size()) {
         return pingIndices.get(i);
      }
      return null;
   }

   private @Nullable DataFile getDataFileOrNull(PingIndex pingIndex) {
      int i = getContainingDataFileIndex(pingIndex);
      return i >= 0 && i < dataFiles.size() ? dataFiles.get(i) : null;
   }

   public DataFile getDataFile(PingIndex pingIndex) {
      DataFile dataFile = getDataFileOrNull(pingIndex);
      if (dataFile == null) {
         throw new IllegalArgumentException(pingIndex.toString());
      }
      return dataFile;
   }

   public int getContainingDataFileIndex(PingIndex pingIndex) {
      int i = Utils.binarySearchForLong(dataFiles, pingIndex.getPingNumber(), dataFile -> dataFile.getPingRange().begin().getPingNumber());
      if (i >= 0) {
         return i;
      }
      int insertionIndex = -(i + 1);
      if (insertionIndex == dataFiles.size()) {
         if (insertionIndex == 0 || pingIndex.compareTo(dataFiles.get(insertionIndex - 1).getPingRange().end()) >= 0) {
            return insertionIndex;
         }
      }
      return insertionIndex - 1;
   }

   public List<DataFile> getDataFiles(PingRange pingRange) {
      if (pingRange.isEmpty()) {
         return List.of();
      }
      int iBegin = Math.max(getContainingDataFileIndex(pingRange.begin()), 0);
      int iEnd = Math.min(getContainingDataFileIndex(previousOrSame(pingRange.end())) + 1, dataFiles.size());
      return dataFiles.subList(iBegin, iEnd);
   }

   @Override
   public List<PingIndex> getPingIndices() {
      return pingIndices;
   }

   @Override
   public List<PingIndex> getPingIndices(Range<PingIndex> pingRange) {
      if (pingRange.isEmpty() || pingIndices.isEmpty()) {
         return List.of();
      }
      long firstPingNumber = pingIndices.getFirst().getPingNumber();
      int begin = Math.max(0, (int) (pingRange.begin().getPingNumber() - firstPingNumber));
      int end = Math.min(pingIndices.size(), (int) (pingRange.end().getPingNumber() - firstPingNumber));
      if (begin >= end) {
         return List.of();
      }
      return pingIndices.subList(begin, end);
   }

   @Override
   public Stream<PingIndex> getPingIndexStream(Range<PingIndex> pingRange) {
      return getPingIndices(pingRange).stream();
   }

   public float getCoordinatedDepth(PingIndex pingIndex) {
      DataFile dataFile = getDataFileOrNull(pingIndex);
      if (dataFile != null) {
         return dataFile.getCoordinatedDepth(pingIndex);
      }
      if (pingIndex.equals(totalPingRange.end())) {
         PingIndex previousPingIndex = previousOrNull(pingIndex);
         if (previousPingIndex == null) {
            return 0;
         }
         return getCoordinatedDepth(previousPingIndex);
      }
      throw new IllegalArgumentException(pingIndex.toString());
   }

   public Bot0Datagram getBot0Datagram(PingIndex pingIndex) {
      int i = pingNumberToIndex(pingIndex.getPingNumber());
      if (i >= 0 && i < bot0Datagrams.size()) {
         return bot0Datagrams.get(i);
      }
      if (pingIndex.equals(totalPingRange.end())) {
         PingIndex previousPingIndex = previousOrNull(pingIndex);
         if (previousPingIndex == null) {
            return new Bot0Datagram(Instant.EPOCH, 0);
         }
         return getBot0Datagram(previousPingIndex);
      }
      throw new IllegalArgumentException(pingIndex.toString());
   }

   public FloatRange getBottomDepthRange(PingRange pingRange, int channel) {
      FloatRangeBuilder builder = new FloatRangeBuilder();
      getPingIndices(pingRange).forEach(pingIndex -> {
         Bot0Datagram bot0Datagram = getBot0Datagram(pingIndex);
         double depth = bot0Datagram.getChannelDepths()[channel - 1];
         builder.expand(depth);
      });
      return builder.toFloatRange();
   }

   public float getMaxDepth() {
      return getMaxDepth(ProgressHandler.ignore());
   }

   public float getMaxDepth(ProgressHandler progressHandler) {
      if (maxDepth == null) {
         maxDepth = computeMaxDepth(progressHandler);
      }
      progressHandler.setProgress(1);
      return maxDepth;
   }

   private float computeMaxDepth(ProgressHandler progressHandler) {
      Listener progressCounter = progressHandler.asCountingListener(dataFiles.size());
      AtomicDouble maxDepth = new AtomicDouble(0);
      dataFiles.parallelStream()
            .forEach(dataFile -> {
               double maxDepthForFile = dataFile.computeMaxDepth(maxDepth.get());
               maxDepth.accumulateAndGet(maxDepthForFile, Math::max);
               progressCounter.listen();
            });
      return (float) maxDepth.get();
   }

   @Override
   public PingIndex getClosestPingIndex(double value, PingMapping pingMapping) {
      return DataUtils.getClosestPingIndex(pingIndices, totalPingRange.end(), value, pingMapping);
   }

   @Override
   public @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping) {
      return DataUtils.getContainingPingIndex(pingIndices, totalPingRange.end(), value, pingMapping);
   }

   void asyncLoadPings(List<PingIndex> pingIndices, AsyncHandle asyncHandle, Runnable onCompletion) {
      pingLoaderExecutor.execute(asyncHandle.createManagedRunnable(() -> {
         int n = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
         Queue<Future<?>> futures = new ArrayDeque<>();
         for (PingIndex pingIndex : pingIndices) {
            futures.add(Exec.CACHED_THREAD_POOL.submit(() -> {
               if (asyncHandle.isCancelled()) {
                  return;
               }
               DataFile dataFile = getDataFileOrNull(pingIndex);
               if (dataFile != null) {
                  Ping ping = dataFile.getPing(pingIndex);
                  PingData pingData = ping.getPingData();
                  pingLoaded(new LoadedPing(ping, pingData));
               }
            }));
            if (futures.size() >= n) {
               Utils.awaitFuture(futures.remove());
            }
         }
         futures.forEach(Utils::awaitFuture);

         if (!asyncHandle.isCancelled()) {
            onCompletion.run();
         }
      }));
   }

   public Ping getPing(PingIndex pingIndex) {
      DataFile dataFile = getDataFileOrNull(pingIndex);
      if (dataFile != null) {
         return dataFile.getPing(pingIndex);
      }
      throw new IllegalArgumentException(pingIndex.toString());
   }

   public void discardLoadedData() {
      dataFiles.forEach(DataFile::discardLoadedData);
   }

   void pingLoaded(LoadedPing loadedPing) {
      dataManagers.forEach(dataManager -> dataManager.pingLoaded(loadedPing));
   }

   public enum Compatibility {
      OK, INCOMPATIBLE_PING_CONFIGURATION, UNUSABLE
   }

   private record DataFileResult(@Nullable DataFile dataFile, @Nullable IOException exception) {
   }
}
