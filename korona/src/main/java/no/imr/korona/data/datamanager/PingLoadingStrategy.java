package no.imr.korona.data.datamanager;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

/**
 * A strategy for selecting an order to load missing pings.
 */
public enum PingLoadingStrategy {
   LEFT_TO_RIGHT {
      @Override
      void addNeededPings(@Nullable Ping[] availablePings, @Nullable PingIndex[] requiredPings, List<PingIndex> neededPings) {
         for (int i = 0; i < requiredPings.length; i++) {
            if (availablePings[i] == null) {
               PingIndex requiredPing = requiredPings[i];
               if (requiredPing != null) {
                  neededPings.add(requiredPing);
               }
            }
         }
      }
   },

   RANDOM {
      @Override
      void addNeededPings(@Nullable Ping[] availablePings, @Nullable PingIndex[] requiredPings, List<PingIndex> neededPings) {
         LEFT_TO_RIGHT.addNeededPings(availablePings, requiredPings, neededPings);
         Collections.shuffle(neededPings);
      }
   },

   LONGEST_GAP_LEFT_TO_RIGHT {
      @Override
      void addNeededPings(@Nullable Ping[] availablePings, @Nullable PingIndex[] requiredPings, List<PingIndex> neededPings) {
         addNeededPingsByGap(availablePings, requiredPings, neededPings,
               Comparator.<Gap>comparingInt(gap -> -gap.size)
                     .thenComparingInt(gap -> gap.min));
      }
   },

   LONGEST_GAP_RANDOM() {
      @Override
      void addNeededPings(@Nullable Ping[] availablePings, @Nullable PingIndex[] requiredPings, List<PingIndex> neededPings) {
         Random random = new Random();
         Map<Gap, Integer> randomValues = new HashMap<>();
         addNeededPingsByGap(availablePings, requiredPings, neededPings,
               Comparator.<Gap>comparingInt(gap -> -gap.size)
                     .thenComparingInt(gap -> randomValues.computeIfAbsent(gap, _ -> random.nextInt())));
      }
   };

   private static void addNeededPingsByGap(@Nullable Ping[] availablePings, @Nullable PingIndex[] requiredPings, List<PingIndex> neededPings, @Nullable Comparator<? super Gap> gapComparator) {
      Queue<Gap> gapQueue = new PriorityQueue<>(availablePings.length, gapComparator);

      int firstMissingInSequence = -1;
      for (int i = 0; i < availablePings.length; i++) {
         if (availablePings[i] == null) {
            if (firstMissingInSequence == -1) {
               firstMissingInSequence = i;
            }
         } else {
            if (firstMissingInSequence != -1) {
               gapQueue.add(new Gap(firstMissingInSequence, i - 1));
               firstMissingInSequence = -1;
            }
         }
      }
      if (firstMissingInSequence != -1) {
         gapQueue.add(new Gap(firstMissingInSequence, availablePings.length - 1));
      }

      while (true) {
         Gap gap = gapQueue.poll();
         if (gap == null) {
            break;
         }

         int i = (gap.min + gap.max) >>> 1; // Overflow safe middle value
         PingIndex requiredPing = requiredPings[i];

         if (requiredPing == null) {
            // Center position has no PingIndex => must find another.
            int gapRadius = (gap.max - gap.min + 1) / 2;
            for (int r = 1; r <= gapRadius; r++) {
               int iTest = i - r;
               if (iTest >= gap.min) {
                  requiredPing = requiredPings[iTest];
                  if (requiredPing != null) {
                     i = iTest;
                     break;
                  }
               }

               iTest = i + r;
               if (iTest <= gap.max) {
                  requiredPing = requiredPings[iTest];
                  if (requiredPing != null) {
                     i = iTest;
                     break;
                  }
               }
            }
         }

         if (requiredPing != null) {
            neededPings.add(requiredPing);

            if (i != gap.min) {
               gapQueue.add(new Gap(gap.min, i - 1));
            }

            if (i != gap.max) {
               gapQueue.add(new Gap(i + 1, gap.max));
            }
         }
      }
   }

   /**
    * Determines the order to load the missing pings.
    *
    * @param availablePings an array of available pings that may contain {@code null} elements
    * @param requiredPings  an array of missing pings that may contain {@code null} elements
    * @param neededPings    a list to append the missing pings to in the order they should be loaded
    */
   abstract void addNeededPings(@Nullable Ping[] availablePings, @Nullable PingIndex[] requiredPings, List<PingIndex> neededPings);

   /**
    * Represents a sequence of missing pings that are not loaded.
    * A gap includes both lower and upper bounds.
    */
   private static final class Gap {
      private final int min;
      private final int max;
      private final int size;

      private Gap(int min, int max) {
         this.min = min;
         this.max = max;
         size = max - min + 1;
      }
   }
}
