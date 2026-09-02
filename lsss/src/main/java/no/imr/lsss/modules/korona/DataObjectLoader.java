package no.imr.lsss.modules.korona;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.TableOfContentsPingItem;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.ExecutorObservation;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.RangeMap;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * For loading data objects based on priority by ping.
 */
public final class DataObjectLoader<T> {
   private final AsyncHandle asyncHandle = new AsyncHandle();
   private final ExecutorObservation executorObservation;
   private final DataFileSet dataFileSet;
   private final DataFilesCache<T> dataFilesCache;
   private final Consumer<? super T> listener;
   private final Class<? extends TableOfContentsPingItem> tableOfContentsClass;
   private final BiFunction<DataFileSet, Ping, Stream<T>> dataObjectExtractor;
   private volatile PingRange priorityPingRange = PingRange.EMPTY_RANGE;

   public DataObjectLoader(ExecutorObservation executorObservation, DataFileSet dataFileSet, DataFilesCache<T> dataFilesCache,
                           Consumer<? super T> listener, Class<? extends TableOfContentsPingItem> tableOfContentsClass,
                           BiFunction<DataFileSet, Ping, Stream<T>> dataObjectExtractor) {
      this.executorObservation = executorObservation;
      this.dataFileSet = dataFileSet;
      this.dataFilesCache = dataFilesCache;
      this.listener = listener;
      this.tableOfContentsClass = tableOfContentsClass;
      this.dataObjectExtractor = dataObjectExtractor;
      executorObservation.execute(Exec.CACHED_THREAD_POOL, asyncHandle.createManagedRunnable(this::doit));
   }

   public void cancel() {
      asyncHandle.cancel();
      asyncHandle.waitUntilFinished();
   }

   public AsyncHandle getAsyncHandle() {
      return asyncHandle;
   }

   private void doit() {
      RangeMap<PingIndex, DataFilesCache.PerDataFileCache<T>> caches = new ArrayRangeMap<>();
      Queue<PingIndex> queue = new PriorityQueue<>();

      for (DataFile dataFile : dataFileSet.getDataFiles()) {
         DataFilesCache.PerDataFileCache<T> cache = dataFilesCache.get(dataFile);
         if (cache == null) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            Ping lastPing = dataFile.getLastPing();
            Set<PingIndex> pingIndices = getPingIndicesToLoad(dataFile, lastPing);
            cache = new DataFilesCache.PerDataFileCache<>(pingIndices);
            dataFilesCache.put(dataFile, cache);
         }

         caches.put(dataFile.getPingRange(), cache);
         queue.addAll(cache.getPingIndices());
         for (T dataObject : cache.getCachedObjects()) {
            publish(dataObject);
         }
      }

      PingRange effectivePriorityPingRange = PingRange.EMPTY_RANGE;
      while (!queue.isEmpty()) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         if (!effectivePriorityPingRange.equals(priorityPingRange)) {
            effectivePriorityPingRange = priorityPingRange;
            PingIndex referencePingIndex = effectivePriorityPingRange.begin();
            Queue<PingIndex> newQueue = new PriorityQueue<>(queue.size(), pingIndexComparator(referencePingIndex));
            newQueue.addAll(queue);
            queue = newQueue;
         }

         PingIndex pingIndex = queue.remove();
         Ping ping = dataFileSet.getPing(pingIndex);
         if (asyncHandle.isCancelled()) {
            return;
         }
         DataFilesCache.PerDataFileCache<T> cache = caches.get(pingIndex);
         if (cache == null) {
            throw new IllegalStateException();
         }
         dataObjectExtractor.apply(dataFileSet, ping).forEach(dataObject -> {
            cache.getCachedObjects().add(dataObject);
            publish(dataObject);
         });
         cache.getPingIndices().remove(pingIndex);
      }
   }

   private Set<PingIndex> getPingIndicesToLoad(DataFile dataFile, Ping lastPing) {
      return lastPing.getPingItems(tableOfContentsClass)
            .flatMap(tableOfContentsPingItem -> tableOfContentsPingItem.getInstants().stream())
            .map(instant -> dataFile.getClosestPingIndex(PingMapping.instantToTimeValue(instant), PingMapping.TIME))
            .collect(Collectors.toSet());
   }

   public void setPriorityPingRange(PingRange priorityPingRange) {
      this.priorityPingRange = priorityPingRange;
   }

   private void publish(T dataObject) {
      executorObservation.execute(Exec.FORK_JOIN_POOL, asyncHandle.createManagedRunnable(() -> listener.accept(dataObject)));
   }

   /**
    * Prioritizes ping indices after the reference ping index.
    */
   private static Comparator<PingIndex> pingIndexComparator(PingIndex referencePingIndex) {
      return (pingIndex1, pingIndex2) -> {
         boolean pingIndex1Before = pingIndex1.compareTo(referencePingIndex) < 0;
         boolean pingIndex2Before = pingIndex2.compareTo(referencePingIndex) < 0;
         if (pingIndex1Before ^ pingIndex2Before) {
            return pingIndex1Before ? 1 : -1;
         }
         return pingIndex1.compareTo(pingIndex2);
      };
   }
}
