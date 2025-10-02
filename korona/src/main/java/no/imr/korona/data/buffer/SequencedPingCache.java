package no.imr.korona.data.buffer;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.CyclicBoundedList;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Caches a contiguous sequence of pings.
 */
final class SequencedPingCache {
   private final int maxSize;
   private final CyclicBoundedList<Ping> pings;
   private final CyclicBoundedList<PingData> pingDatas;
   private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

   SequencedPingCache(int maxSize) {
      this.maxSize = maxSize;
      pings = new CyclicBoundedList<>(maxSize);
      pingDatas = new CyclicBoundedList<>(maxSize);
   }

   int getMaxSize() {
      return maxSize;
   }

   boolean isEmpty() {
      return pings.isEmpty();
   }

   @Nullable Ping getPing(PingIndex pingIndex) {
      Lock lock = readWriteLock.readLock();
      lock.lock();
      try {
         if (pings.isEmpty()) {
            return null;
         }
         int i = getIndex(pingIndex);
         return i >= 0 && i < pings.size() ? pings.get(i) : null;
      } finally {
         lock.unlock();
      }
   }

   int getIndex(PingIndex pingIndex) {
      Lock lock = readWriteLock.readLock();
      lock.lock();
      try {
         return (int) (pingIndex.getPingNumber() - getFirst().getPingNumber());
      } finally {
         lock.unlock();
      }
   }

   Ping getLast() {
      Lock lock = readWriteLock.readLock();
      lock.lock();
      try {
         return pings.getLast();
      } finally {
         lock.unlock();
      }
   }

   Ping getFirst() {
      Lock lock = readWriteLock.readLock();
      lock.lock();
      try {
         return pings.getFirst();
      } finally {
         lock.unlock();
      }
   }

   void addLast(Ping ping) {
      Lock lock = readWriteLock.writeLock();
      lock.lock();
      try {
         assert pings.isEmpty() || getLast().getPingNumber() + 1 == ping.getPingNumber() : ping + " " + pings;
         pings.add(ping);
         pingDatas.add(ping.getPingData());
      } finally {
         lock.unlock();
      }
   }

   void addFirst(Ping ping) {
      Lock lock = readWriteLock.writeLock();
      lock.lock();
      try {
         assert pings.isEmpty() || getFirst().getPingNumber() - 1 == ping.getPingNumber() : ping + " " + pings;
         pings.addFirst(ping);
         pingDatas.addFirst(ping.getPingData());
      } finally {
         lock.unlock();
      }
   }

   void clear() {
      Lock lock = readWriteLock.writeLock();
      lock.lock();
      try {
         pings.clear();
         pingDatas.clear();
      } finally {
         lock.unlock();
      }
   }
}
