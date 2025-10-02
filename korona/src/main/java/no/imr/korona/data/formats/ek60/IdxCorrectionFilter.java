package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DatagramSource;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.ping.WrapAround;
import no.imr.tools.math.Median;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

/**
 * A filter which produces a valid sequence of {@link Idx0Datagram}s.
 * <ul>
 * <li>Datagrams other than {@link Idx0Datagram} are ignored.
 * <li>Ping numbering is consecutive.
 * <li>Vessel distance is non-decreasing.
 * </ul>
 */
public final class IdxCorrectionFilter implements IdxSource {
   private final NonIdxFilter nonIdxFilter;
   private final StandardCorrectionFilter standardCorrectionFilter;
   private final VesselDistanceSpikeFilter vesselDistanceSpikeFilter;
   private final TimeCorrectionFilter timeCorrectionFilter;
   private final IdxSource totalFilter;

   public IdxCorrectionFilter(DatagramSource datagramSource) {
      nonIdxFilter = new NonIdxFilter(datagramSource);
      vesselDistanceSpikeFilter = new VesselDistanceSpikeFilter(nonIdxFilter);
      standardCorrectionFilter = new StandardCorrectionFilter(vesselDistanceSpikeFilter);
      timeCorrectionFilter = new TimeCorrectionFilter(standardCorrectionFilter);
      totalFilter = timeCorrectionFilter;
   }

   List<BaseDatagram> getOtherDatagrams() {
      return nonIdxFilter.getOtherIdxDatagrams();
   }

   int getMissingCount() {
      return standardCorrectionFilter.getMissingCount();
   }

   int getIgnoredCount() {
      return standardCorrectionFilter.getIgnoredCount();
   }

   int getSpikeCount() {
      return vesselDistanceSpikeFilter.getCorrectionCount();
   }

   @Nullable WrapAround getWrapAround() {
      return vesselDistanceSpikeFilter.getWrapAround();
   }

   int getTimeCorrectionCount() {
      return timeCorrectionFilter.getCorrectionCount();
   }

   @Override
   public @Nullable Idx0Datagram nextDatagram() throws IOException {
      return totalFilter.nextDatagram();
   }

   /**
    * Removes datagram that are not {@link Idx0Datagram}.
    */
   private static final class NonIdxFilter implements IdxSource {
      private final DatagramSource datagramSource;
      private final List<BaseDatagram> otherIdxDatagrams = new ArrayList<>();

      private NonIdxFilter(DatagramSource datagramSource) {
         this.datagramSource = datagramSource;
      }

      private List<BaseDatagram> getOtherIdxDatagrams() {
         return otherIdxDatagrams;
      }

      @Override
      public @Nullable Idx0Datagram nextDatagram() throws IOException {
         while (true) {
            BaseDatagram datagram = datagramSource.nextDatagram();
            switch (datagram) {
               case null -> {
                  return null;
               }
               case Idx0Datagram idx0Datagram -> {
                  return idx0Datagram;
               }
               default -> {
                  otherIdxDatagrams.add(datagram);
               }
            }
         }
      }
   }

   /**
    * Corrects ping numbering and vessel distance.
    */
   private static final class StandardCorrectionFilter implements IdxSource {
      private final IdxSource idxSource;

      private int missingCount;
      private int ignoredCount;

      private @Nullable Idx0Datagram previousIdx;
      private final Queue<Idx0Datagram> queue = new ArrayDeque<>();

      private StandardCorrectionFilter(IdxSource idxSource) {
         this.idxSource = idxSource;
      }

      private int getMissingCount() {
         return missingCount;
      }

      private int getIgnoredCount() {
         return ignoredCount;
      }

      @Override
      public @Nullable Idx0Datagram nextDatagram() throws IOException {
         if (queue.isEmpty()) {
            Idx0Datagram idx = nextIdx();
            if (idx == null) {
               return null;
            }

            if (previousIdx != null) {
               modify(idx);
            } else {
               previousIdx = new Idx0Datagram(idx);
            }

            previousIdx.copyFrom(idx);
            queue.add(idx);
         }
         return queue.remove();
      }

      private void modify(Idx0Datagram idx) {
         if (previousIdx == null) {
            return;
         }
         long expectedPingNumber = previousIdx.getPingNumber() + 1;
         if (idx.getPingNumber() != expectedPingNumber) {
            assert idx.getPingNumber() > expectedPingNumber;

            for (long missingPingNumber = expectedPingNumber; missingPingNumber < idx.getPingNumber(); missingPingNumber++) {
               Idx0Datagram missingIdx = new Idx0Datagram(previousIdx);
               missingIdx.setPingNumber(missingPingNumber);
               queue.add(missingIdx);
               missingCount++;
            }
         }

         if (idx.getVesselDistance() < previousIdx.getVesselDistance()) {
            idx.setVesselDistance(previousIdx.getVesselDistance());
         }
      }

      private @Nullable Idx0Datagram nextIdx() throws IOException {
         while (true) {
            Idx0Datagram idx = idxSource.nextDatagram();
            if (idx == null) {
               return null;
            }
            if (previousIdx != null && previousIdx.getPingNumber() >= idx.getPingNumber()) {
               // Skip idx when ping number does not increase
               ignoredCount++;
               continue;
            }
            return idx;
         }
      }
   }

   private abstract static class BaseBufferedFilter implements IdxSource {
      private final IdxSource idxSource;
      final Deque<Idx0Datagram> inputBuffer = new ArrayDeque<>();

      private BaseBufferedFilter(IdxSource idxSource) {
         this.idxSource = idxSource;
      }

      @Nullable Idx0Datagram nextInputIdx() throws IOException {
         if (inputBuffer.isEmpty()) {
            return idxSource.nextDatagram();
         } else {
            return inputBuffer.pop();
         }
      }
   }

   /**
    * Removes spikes in vessel distance.
    */
   private static final class VesselDistanceSpikeFilter extends BaseBufferedFilter {
      private static final double SPIKE_THRESHOLD = 99;

      private boolean initialized;
      private double referenceVesselDistance;
      private int correctionCount;
      private @Nullable WrapAround wrapAround;

      private VesselDistanceSpikeFilter(IdxSource idxSource) {
         super(idxSource);
      }

      private int getCorrectionCount() {
         return correctionCount;
      }

      private @Nullable WrapAround getWrapAround() {
         return wrapAround;
      }

      @Override
      public @Nullable Idx0Datagram nextDatagram() throws IOException {
         if (!initialized) {
            referenceVesselDistance = initializeReference();
            initialized = true;
         }

         Idx0Datagram idx = nextInputIdx();
         if (idx == null) {
            return null;
         }

         correctForWrapAround(idx);

         if (Math.abs(idx.getVesselDistance() - referenceVesselDistance) < SPIKE_THRESHOLD) {
            referenceVesselDistance = idx.getVesselDistance();
         } else {
            if (wrapAround == null && WrapAround.isWrapAround(referenceVesselDistance, idx.getVesselDistance())) {
               wrapAround = new WrapAround(idx, WrapAround.roundToPowerOfTen(referenceVesselDistance));
               correctForWrapAround(idx);
               referenceVesselDistance = idx.getVesselDistance();
            } else {
               idx.setVesselDistance(referenceVesselDistance);
               correctionCount++;
            }
         }
         return idx;
      }

      private void correctForWrapAround(Idx0Datagram idx) {
         if (wrapAround != null && idx.getNTDate() >= wrapAround.pingIndex().getNTDate()) {
            idx.setVesselDistance(idx.getVesselDistance() + wrapAround.vesselDistance());
         }
      }

      private double initializeReference() throws IOException {
         List<Idx0Datagram> idxs = new ArrayList<>();
         int maxN = 9;
         double[] distances = new double[maxN];
         double referenceDistance = Double.NaN;

         for (int n = 3; n <= maxN; n += 2) {
            while (idxs.size() < n) {
               Idx0Datagram nextInputIdx = nextInputIdx();
               if (nextInputIdx == null) {
                  break;
               }
               distances[idxs.size()] = nextInputIdx.getVesselDistance();
               idxs.add(nextInputIdx);
            }
            if (idxs.isEmpty()) {
               break;
            }
            referenceDistance = Median.quickSelect(distances, 0, idxs.size());
            if (idxs.size() < n || n >= maxN) {
               break;
            }

            int closeToReferenceCount = 0;
            for (Idx0Datagram idx : idxs) {
               if (Math.abs(idx.getVesselDistance() - referenceDistance) < SPIKE_THRESHOLD) {
                  closeToReferenceCount++;
               }
            }

            if (closeToReferenceCount >= n / 2 + 2) {
               break;
            }
         }

         if (!idxs.isEmpty()) {
            Idx0Datagram firstIdx = idxs.getFirst();
            if (WrapAround.isWrapAround(firstIdx.getVesselDistance(), referenceDistance)) {
               referenceDistance = firstIdx.getVesselDistance();
               for (Idx0Datagram idx : idxs) {
                  if (WrapAround.isWrapAround(referenceDistance, idx.getVesselDistance())) {
                     wrapAround = new WrapAround(idx, WrapAround.roundToPowerOfTen(referenceDistance));
                     break;
                  }
               }
            }
         }

         for (ListIterator<Idx0Datagram> it = idxs.listIterator(idxs.size()); it.hasPrevious(); ) {
            inputBuffer.push(it.previous());
         }
         return referenceDistance;
      }
   }

   private static final class TimeCorrectionFilter extends BaseBufferedFilter {
      private int correctionCount;
      private long previousNTDate;
      private double previousVesselDistance;

      private TimeCorrectionFilter(IdxSource idxSource) {
         super(idxSource);
      }

      private int getCorrectionCount() {
         return correctionCount;
      }

      @Override
      public @Nullable Idx0Datagram nextDatagram() throws IOException {
         Idx0Datagram idx = nextInputIdx();
         if (idx == null) {
            return null;
         }
         if (previousNTDate >= idx.getNTDate()) {
            correct(idx);
            correctionCount++;
         }
         previousNTDate = idx.getNTDate();
         previousVesselDistance = idx.getVesselDistance();
         return idx;
      }

      private void correct(Idx0Datagram idx) throws IOException {
         long newNTDate;
         Idx0Datagram next = nextInputIdx();
         if (next == null) {
            newNTDate = previousNTDate + 1;
         } else {
            long prevNTDate = previousNTDate;
            long nextNTDate = next.getNTDate();

            double prevDist = idx.getVesselDistance() - previousVesselDistance;
            double nextDist = next.getVesselDistance() - idx.getVesselDistance();
            double weightSum = nextDist + prevDist;
            if (weightSum == 0) {
               newNTDate = prevNTDate + 1;
            } else {
               newNTDate = (long) ((nextDist * prevNTDate + prevDist * nextNTDate) / weightSum);
               newNTDate = Math.min(newNTDate, nextNTDate - 1);
               newNTDate = Math.max(newNTDate, prevNTDate + 1);
            }

            inputBuffer.push(next);
         }
         idx.setNTDate(newNTDate);
      }
   }
}
