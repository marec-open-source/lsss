package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.ping.ExtrapolatedPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class IdxFileAdjuster {
   private @Nullable PingIndex lastPingIndex;
   private @Nullable PingIndex extrapolatedPingIndex;

   public IdxFileAdjuster() {
   }

   public long getLastPingNumber() {
      return lastPingIndex != null ? lastPingIndex.getPingNumber() : 0;
   }

   public void setLast(List<Idx0Datagram> lastIdx0Datagrams) {
      lastPingIndex = lastIdx0Datagrams.getLast();
      extrapolatedPingIndex = ExtrapolatedPingIndex.create(lastIdx0Datagrams);
   }

   public void adjustNext(IdxFile idxFile) {
      if (lastPingIndex != null && extrapolatedPingIndex != null) {
         Idx0Datagram firstIdx = idxFile.idx0Datagrams().getFirst();

         if (!firstIdx.getInstant().isAfter(lastPingIndex.getInstant())) {
            Instant nextInstant = extrapolatedPingIndex.getInstant();
            long nanosShift = firstIdx.getInstant().until(nextInstant, ChronoUnit.NANOS);
            Log.global.info(idxFile.file().getFileName()
                  + ": Non-increasing time, shifting with " + nanosShift / 1e9 + " seconds");
            for (Idx0Datagram idx0Datagram : idxFile.idx0Datagrams()) {
               idx0Datagram.setInstant(idx0Datagram.getInstant().plusNanos(nanosShift));
            }
         }

         if (firstIdx.getPingNumber() != lastPingIndex.getPingNumber() + 1) {
            long nextPingNumber = lastPingIndex.getPingNumber() + 1;
            long pingNumberShift = nextPingNumber - firstIdx.getPingNumber();
            Log.global.info(idxFile.file().getFileName()
                  + ": Non-consecutive ping number, shifting with " + pingNumberShift);
            for (Idx0Datagram idx0Datagram : idxFile.idx0Datagrams()) {
               idx0Datagram.setPingNumber(idx0Datagram.getPingNumber() + pingNumberShift);
            }
         }

         if (firstIdx.getVesselDistance() < lastPingIndex.getVesselDistance()) {
            double nextVesselDistance = extrapolatedPingIndex.getVesselDistance();
            double vesselDistanceShift = nextVesselDistance - firstIdx.getVesselDistance();
            Log.global.info(idxFile.file().getFileName()
                  + ": Decreasing vessel distance, shifting with " + vesselDistanceShift);
            for (Idx0Datagram idx0Datagram : idxFile.idx0Datagrams()) {
               idx0Datagram.setVesselDistance(idx0Datagram.getVesselDistance() + vesselDistanceShift);
            }
         }
      }
      setLast(idxFile.idx0Datagrams());
   }
}
