package no.imr.korona.data.datamanager;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.Listeners;
import no.imr.tools.range.RangeUtils;
import no.imr.tools.time.Stopwatch;
import org.jspecify.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * For selecting a subset of approximately equidistant pings from a {@link PingRange}.
 */
public final class PingSampler {
   private static final double NOTIFICATION_INTERVAL_SECONDS = 0.5;

   private final SerialExecutor executor = new SerialExecutor(Exec.FORK_JOIN_POOL);
   private final DataManager dataManager;
   private EchogramPingSettings pingSettings;
   private final ArgChangeManager<List<Ping>> newPingsChangeManager = new ArgChangeManager<>();

   private volatile Data data;

   public PingSampler(DataManager dataManager, EchogramPingSettings pingSettings) {
      this.dataManager = dataManager;
      this.pingSettings = pingSettings;
      data = new Data(PingLoadingStrategy.LONGEST_GAP_LEFT_TO_RIGHT, dataManager.getDataFileSet());

      dataManager.getPingLoadedChangeManager().addListener(Listeners.inExecutor(executor, pair -> {
         data.loadedPing(pair.first(), pair.second());
      }));
   }

   public void setPingSettings(EchogramPingSettings pingSettings) {
      this.pingSettings = pingSettings;
   }

   public void requestPings(PingLoadingStrategy pingLoadingStrategy) {
      cancelPingRequest();
      data = new Data(pingLoadingStrategy, dataManager.getDataFileSet());
   }

   public void cancelPingRequest() {
      data.asyncHandle.cancel();
   }

   public void waitForPingRequest() {
      data.asyncHandle.waitUntilFinished();
   }

   public List<PingIndex> getRequestedPingIndices() {
      return data.requestedPingIndices;
   }

   public List<PingIndex> getRequestedPingIndices(PingRange pingRange) {
      return RangeUtils.subList(data.requestedPingIndices, pingRange);
   }

   public List<Ping> getAvailablePings() {
      return data.availablePings;
   }

   public ArgChangeManager<List<Ping>> getNewPingsChangeManager() {
      return newPingsChangeManager;
   }

   @Override
   public String toString() {
      return pingSettings.getPingRange().toString();
   }

   private final class Data {
      private final AsyncHandle asyncHandle = new AsyncHandle();
      private final @Nullable Ping[] sampledPings;
      @SuppressWarnings("MismatchedReadAndWriteOfArray")
      private final SoftReference<?>[] softPingData;

      private final List<PingIndex> requestedPingIndices;
      private final Stopwatch notificationStopwatch = Stopwatch.createStarted();

      private List<Ping> newPings = new ArrayList<>();
      private volatile List<Ping> availablePings;

      private Data(PingLoadingStrategy pingLoadingStrategy, DataFileSet dataFileSet) {
         int width = pingSettings.getWidth();
         sampledPings = new Ping[width];
         softPingData = new SoftReference<?>[width];

         PingRange pingRange = pingSettings.getPingRange();
         if (pingRange.isEmpty() || width == 0) {
            requestedPingIndices = List.of();
            availablePings = List.of();
            return;
         }

         @Nullable PingIndex[] sampledPingIndices = new PingIndex[width];

         AtomicReferenceArray<@Nullable Ping> atomicSampledPings = new AtomicReferenceArray<>(sampledPings.length);
         dataFileSet.getDataFiles().parallelStream()
               .filter(dataFile -> dataFile.getPingRange().intersects(pingRange))
               .forEach(dataFile -> dataFile.getAvailablePings(pingRange, (ping, pingData) -> {
                  int i = toIntIndex(ping.getPingIndex());
                  if (i < 0 || i >= sampledPings.length) {
                     return;
                  }
                  if (!atomicSampledPings.compareAndSet(i, null, ping)) {
                     return;
                  }
                  sampledPings[i] = ping;
                  sampledPingIndices[i] = ping.getPingIndex();
                  softPingData[i] = new SoftReference<>(pingData);
               }));
         Arrays.stream(sampledPings)
               .filter(Objects::nonNull)
               .forEach(newPings::add);
         availablePings = List.copyOf(newPings);

         fillInMissing(dataFileSet, sampledPingIndices);
         requestedPingIndices = Arrays.stream(sampledPingIndices)
               .filter(Objects::nonNull)
               .toList();

         data = this; // Assign to data field before requesting pings.
         List<PingIndex> neededPings = new ArrayList<>();
         pingLoadingStrategy.addNeededPings(sampledPings, sampledPingIndices, neededPings);
         dataFileSet.asyncLoadPings(neededPings, asyncHandle, () -> {
            executor.execute(asyncHandle.createManagedRunnable(() -> notifyPingSamplerListenersNewPings(true)));
         });
         executor.execute(asyncHandle.createManagedRunnable(() -> notifyPingSamplerListenersNewPings(true)));
      }

      private void fillInMissing(DataFileSet dataFileSet, @Nullable PingIndex[] pingIndices) {
         if (toIntIndex(pingSettings.getPingRange().end()) == 0) {
            // Degenerated ping range with respect to current ping mapping. This can happen for stationary data.
            if (pingIndices[0] == null) {
               // No pings loaded => request the last real ping index.
               pingIndices[0] = dataFileSet.previousOrSame(pingSettings.getPingRange().end());
            }
            return;
         }

         PingIndex totalEndPingIndex = dataFileSet.getTotalRange().end();
         for (int i = 0; i < pingIndices.length; i++) {
            if (pingIndices[i] == null) {
               PingIndex pingIndex = toPingIndex(i);
               int j = toIntIndex(pingIndex);
               if (j >= 0 && j < pingIndices.length && pingIndices[j] == null && !pingIndex.equals(totalEndPingIndex)) {
                  pingIndices[j] = pingIndex;
               }
            }
         }
      }

      private PingIndex toPingIndex(int index) {
         return pingSettings.xToClosestPingIndex(index);
      }

      private int toIntIndex(PingIndex pingIndex) {
         return pingSettings.pingIndexToXIndex(pingIndex);
      }

      private void loadedPing(Ping ping, PingData pingData) {
         int i = toIntIndex(ping.getPingIndex());
         if (i < 0 || i >= sampledPings.length) {
            return;
         }
         Ping sampledPing = sampledPings[i];
         if (sampledPing != null) {
            if (sampledPing == ping) {
               softPingData[i] = new SoftReference<>(pingData);
            }
            return;
         }
         sampledPings[i] = ping;
         softPingData[i] = new SoftReference<>(pingData);
         newPings.add(ping);

         notifyPingSamplerListenersNewPings(false);
      }

      private void notifyPingSamplerListenersNewPings(boolean now) {
         if (!newPings.isEmpty()
               && (now || notificationStopwatch.seconds() >= NOTIFICATION_INTERVAL_SECONDS)) {
            notificationStopwatch.restart();
            newPings.sort(null);
            availablePings = Arrays.stream(sampledPings)
                  .filter(Objects::nonNull)
                  .toList();
            newPingsChangeManager.notifyListeners(newPings);
            newPings = new ArrayList<>();
         }
      }
   }
}
