package no.imr.korona.data.buffer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.feature.EchogramWindow;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.ReloadablePing;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.track.Segment;
import no.imr.korona.data.track.Track;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.misc.ErrorHandler;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A ping buffer backed by a {@link Track}.
 */
public final class TrackPingBuffer extends PingBuffer {
   private static final int CACHE_BUFFER = 100;

   private final Track track;
   private final boolean deleteAngles;

   private final SequencedPingCache cache = new SequencedPingCache(2000);

   private final Cache<PingIndex, PingData> recentPingData = CacheBuilder.newBuilder()
         .maximumSize(2000)
         .build();
   private final LoadingCache<PingIndex, Ping> recentPings = CacheBuilder.newBuilder()
         .maximumSize(2000)
         .build(new CacheLoader<>() {
            @Override
            public Ping load(PingIndex pingIndex) throws IOException {
               Ping ping = getPingFromTrack(pingIndex, new AsyncHandle());
               recentPingData.put(pingIndex, ping.getPingData());
               return ping;
            }
         });

   private final BlockingQueue<PingIndex> pingsToLoad = new LinkedBlockingQueue<>();

   private final int width = cache.getMaxSize() - 2 * CACHE_BUFFER - 1;

   private ErrorHandler errorHandler = ErrorHandler.logging();

   public TrackPingBuffer(Track track, boolean deleteAngles) {
      this.track = track;
      this.deleteAngles = deleteAngles;
      Executor executor = Executors.newSingleThreadExecutor(Exec.newThreadFactory("TrackPingBuffer-PreLoader"));
      executor.execute(new Preloader());
   }

   public void setErrorHandler(ErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
   }

   @Override
   public void newPing(Ping ping) {
      ping = preparePing(ping);
      recentPings.put(ping.getPingIndex(), ping);
      recentPingData.put(ping.getPingIndex(), ping.getPingData());
      super.newPing(ping);
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return track.getPingConfiguration();
   }

   @Override
   public PingRange getTotalRange() {
      return track.getTotalRange();
   }

   @Override
   public PingIndex getClosestPingIndex(double value, PingMapping pingMapping) {
      return track.getClosestPingIndex(value, pingMapping);
   }

   @Override
   public @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping) {
      return track.getContainingPingIndex(value, pingMapping);
   }

   @Override
   public @Nullable Ping getPing(double value, PingMapping pingMapping) {
      return getPing(value, pingMapping, true);
   }

   @Override
   public @Nullable Ping getPing(double value, PingMapping pingMapping, boolean adjustCache) {
      try {
         PingIndex pingIndex = track.getContainingPingIndex(value, pingMapping);
         if (pingIndex == null) {
            return null;
         }

         Ping ping = getPingFromCache(pingIndex, adjustCache);

         if (ping == null) {
            ping = recentPings.get(pingIndex);
         }

         return ping;
      } catch (ExecutionException e) {
         errorHandler.onError(e);
         return null;
      }
   }

   @Override
   public boolean isReady(PingIndex pingIndex) {
      PingIndex firstPingIndex = getPingIndex(pingIndex, -width + 1);
      return getPingFromCache(pingIndex, true) != null && (firstPingIndex == null || getPingFromCache(firstPingIndex, true) != null);
   }

   private @Nullable Ping getPingFromCache(PingIndex pingIndex, boolean adjustCache) {
      if (adjustCache) {
         pingsToLoad.clear();
         pingsToLoad.add(pingIndex);
      }
      return cache.getPing(pingIndex);
   }

   public Track getTrack() {
      return track;
   }

   private Ping getPingFromTrack(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      Ping ping = track.getPing(pingIndex, asyncHandle);
      return preparePing(ping);
   }

   private Ping preparePing(Ping ping) {
      if (deleteAngles) {
         removeAnglesFromPing(ping);
      }
      Segment segment = track.getSegment(ping.getPingIndex());
      if (segment == null) {
         return new DefaultPing(ping.getPingConfiguration(), ping.getPingIndex(), ping.getBot0Datagram());
      }
      return new ReloadablePing(segment.getSegmentData(), ping);
   }

   private static void removeAnglesFromPing(Ping ping) {
      ping.getNonNullPowerDatas().forEach(PowerData::removeAngles);
   }

   public EchogramWindow getEchogramWindow(Configurator configurator, PingRange pingRange, FloatRange depthRange) {
      Segment segment = track.getSegment(pingRange.begin());
      if (segment == null) {
         return new EchogramWindow(configurator, 0, List.of(), depthRange);
      }
      int pingOffset = (int) (segment.getPingRange().begin().getPingNumber() - pingRange.begin().getPingNumber());
      int pingCount = pingRange.getPingCount();
      List<Ping> pings = new ArrayList<>(pingCount);
      for (int i = 0; i < pingCount; i++) {
         Ping ping = getPing(pingRange.begin().getPingNumber() + i, PingMapping.NUMBER, false);
         if (ping != null) {
            pings.add(ping);
         }
      }
      return new EchogramWindow(configurator, pingOffset, pings, depthRange);
   }

   /**
    * Loads pings in the background.
    */
   private final class Preloader implements Runnable {
      private final AsyncHandle asyncHandle = new AsyncHandle();
      private long targetPingNumber;

      private Preloader() {
      }

      @Override
      public void run() {
         boolean didStep = false;
         while (!asyncHandle.isCancelled()) {
            try {
               if (!pingsToLoad.isEmpty() || !didStep) {
                  PingIndex pingIndex = pingsToLoad.take();
                  updateTarget(pingIndex);
               }
               didStep = step();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               break;
            } catch (IOException e) {
               errorHandler.onError(e);
            }
         }
      }

      private void updateTarget(PingIndex pingIndex) {
         long pingNumber = pingIndex.getPingNumber();

         if (cache.isEmpty()) {
            targetPingNumber = pingNumber;
         } else if (pingNumber < cache.getFirst().getPingNumber() + CACHE_BUFFER) {
            targetPingNumber = Math.min(targetPingNumber, pingNumber);
         } else if (pingNumber > cache.getLast().getPingNumber() - CACHE_BUFFER) {
            targetPingNumber = Math.max(targetPingNumber, pingNumber);
         }
      }

      private boolean step() throws IOException {
         boolean didStep = false;

         if (cache.isEmpty() || targetPingNumber < cache.getFirst().getPingNumber() - width || targetPingNumber > cache.getLast().getPingNumber() + width) {
            cache.clear();
            Ping ping = loadPing(targetPingNumber);
            if (ping == null) {
               return false;
            }
            cache.addLast(ping);
            didStep = true;
         }

         if (targetPingNumber < cache.getFirst().getPingNumber() + CACHE_BUFFER) {
            didStep |= stepBack();
         }

         if (targetPingNumber > cache.getLast().getPingNumber() - CACHE_BUFFER) {
            didStep |= stepForward();
         }

         return didStep;
      }

      private boolean stepBack() throws IOException {
         long pingNumber = cache.getFirst().getPingNumber() - 1;
         Ping ping = loadPing(pingNumber);
         if (ping != null) {
            cache.addFirst(ping);
            return true;
         } else {
            return false;
         }
      }

      private boolean stepForward() throws IOException {
         long pingNumber = cache.getLast().getPingNumber() + 1;
         Ping ping = loadPing(pingNumber);
         if (ping != null) {
            cache.addLast(ping);
            return true;
         } else {
            return false;
         }
      }

      private @Nullable Ping loadPing(long pingNumber) throws IOException {
         PingIndex pingIndex = track.getContainingPingIndex(pingNumber, PingMapping.NUMBER);
         if (pingIndex == null) {
            return null;
         }
         Ping ping = recentPings.getIfPresent(pingIndex);
         if (ping != null) {
            return ping;
         }
         return getPingFromTrack(pingIndex, asyncHandle);
      }
   }
}
