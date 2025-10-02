package no.imr.lsss.modules.korona;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.ping.PingIndex;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A weak map from DataFile to all the objects of type T it contains.
 */
public final class DataFilesCache<T> {
   private final Map<DataFile, PerDataFileCache<T>> cache = new WeakHashMap<>();

   public DataFilesCache() {
   }

   void put(DataFile dataFile, PerDataFileCache<T> perDataFileCache) {
      cache.put(dataFile, perDataFileCache);
   }

   @Nullable PerDataFileCache<T> get(DataFile dataFile) {
      return cache.get(dataFile);
   }

   static final class PerDataFileCache<T> {
      private final Set<PingIndex> pingIndices;
      private final List<T> cachedObjects = new ArrayList<>();

      PerDataFileCache(Set<PingIndex> pingIndices) {
         this.pingIndices = pingIndices;
      }

      Set<PingIndex> getPingIndices() {
         return pingIndices;
      }

      List<T> getCachedObjects() {
         return cachedObjects;
      }
   }
}
