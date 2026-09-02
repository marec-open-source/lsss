package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DataException;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.ping.PingIndexCorrectionOptions;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Displays a {@link WorkerDialog} while creating idx files.
 */
public final class CreateMissingIdxGUI {
   private final DatagramTypeManager datagramTypeManager;
   private final List<EK60SegmentHandle> allSegmentHandles;
   private final List<EK60SegmentHandle> missingIdxSegmentHandles;
   private final ProgressView progressView;
   private final PingIndexCorrectionOptions pingIndexCorrectionOptions;
   private final boolean collapseSequentialPinging;

   public CreateMissingIdxGUI(DatagramTypeManager datagramTypeManager, Collection<? extends SegmentHandle> segmentHandles,
                              PingIndexCorrectionOptions pingIndexCorrectionOptions, boolean collapseSequentialPinging) {
      this.datagramTypeManager = datagramTypeManager;
      allSegmentHandles = Utils.getAllOfType(segmentHandles, EK60SegmentHandle.class).toList();
      missingIdxSegmentHandles = EK60Utils.getEK60WithMissingIdx(allSegmentHandles).toList();
      progressView = new ProgressView("Creating idx files...", missingIdxSegmentHandles.size())
            .useSecondaryProgress();
      this.pingIndexCorrectionOptions = pingIndexCorrectionOptions;
      this.collapseSequentialPinging = collapseSequentialPinging;
   }

   public void start(@Nullable JComponent referenceComponent) {
      new WorkerDialog(referenceComponent, progressView.getComponent())
            .start(new CreateIdxWorker()::doWork);
   }

   private final class CreateIdxWorker {
      private int allSegmentHandlesIndex;

      private CreateIdxWorker() {
      }

      private void doWork(AsyncHandle asyncHandle) {
         IdxFile previousIdxFile = null;
         for (int i = 0; i < missingIdxSegmentHandles.size(); i++) {
            EK60SegmentHandle ek60SegmentHandle = missingIdxSegmentHandles.get(i);
            if (asyncHandle.isCancelled()) {
               return;
            }

            progressView.setMainProgress(i, ek60SegmentHandle.getEK60FileSet().getIdx().getFileName().toString());

            try {
               IdxFile idxFile = EK60Utils.createIdxFile(datagramTypeManager, ek60SegmentHandle.getEK60FileSet(), 1, asyncHandle,
                     progressView.getSecondaryProgressHandler(), pingIndexCorrectionOptions, collapseSequentialPinging);
               if (idxFile == null) {
                  return;
               }
               PingRange previousPingRange = getPreviousPingRange(ek60SegmentHandle, previousIdxFile, asyncHandle);
               if (previousPingRange != null) {
                  adjust(idxFile, previousPingRange);
               }
               if (asyncHandle.isCancelled()) {
                  return;
               }
               FileUtils.replaceFileSafely(idxFile.file(), idxFile.toBytes());
               previousIdxFile = idxFile;
            } catch (Exception e) {
               Log.global.log(Level.WARNING, "Error creating idx file for " + ek60SegmentHandle.getDisplayName(), e);
            }
         }
      }

      private static void adjust(IdxFile idxFile, PingRange previousPingRange) {
         if (previousPingRange.isEmpty()) {
            return;
         }
         List<Idx0Datagram> idxDatagrams = idxFile.idx0Datagrams();
         if (idxDatagrams.isEmpty()) {
            return;
         }
         long pingNumberShift = getPingNumberShift(previousPingRange, idxDatagrams);
         double vesselDistanceShift = getVesselDistanceShift(previousPingRange, idxDatagrams);
         idxDatagrams.forEach(idx -> {
            idx.setPingNumber(idx.getPingNumber() + pingNumberShift);
            idx.setVesselDistance(idx.getVesselDistance() + vesselDistanceShift);
         });
      }

      private static long getPingNumberShift(PingRange previousPingRange, List<Idx0Datagram> idxDatagrams) {
         long startPingNumber = previousPingRange.end().getPingNumber();
         if (Math.abs(TimeUtils.toSeconds(previousPingRange.end().getInstant(), idxDatagrams.getFirst().getInstant())) > 60) {
            startPingNumber++;
         }
         return startPingNumber - idxDatagrams.getFirst().getPingNumber();
      }

      private static double getVesselDistanceShift(PingRange previousPingRange, List<Idx0Datagram> idxDatagrams) {
         if (idxDatagrams.getFirst().getVesselDistance() >= previousPingRange.end().getVesselDistance()) {
            return 0;
         }
         double startVesselDistance = previousPingRange.end().getVesselDistance();
         if (idxDatagrams.size() > 1) {
            int n = Math.min(10, idxDatagrams.size() - 1);
            double pingToPingDistance = (idxDatagrams.get(n).getVesselDistance() - idxDatagrams.getFirst().getVesselDistance()) / n;
            startVesselDistance += pingToPingDistance;
         }
         return startVesselDistance - idxDatagrams.getFirst().getVesselDistance();
      }

      private @Nullable PingRange getPreviousPingRange(EK60SegmentHandle segmentHandle, @Nullable IdxFile previousIdxFile, AsyncHandle asyncHandle) {
         while (allSegmentHandles.get(allSegmentHandlesIndex) != segmentHandle) {
            allSegmentHandlesIndex++;
         }

         for (int i = allSegmentHandlesIndex - 1; i >= 0; i--) {
            if (asyncHandle.isCancelled()) {
               break;
            }

            EK60SegmentHandle previousSegmentHandle = allSegmentHandles.get(i);
            Path previousIdx = previousSegmentHandle.getEK60FileSet().getIdx();
            if (previousIdxFile != null && previousIdxFile.file().equals(previousIdx)) {
               List<Idx0Datagram> idx0Datagrams = previousIdxFile.idx0Datagrams();
               if (!idx0Datagrams.isEmpty()) {
                  return EK60Utils.firstAndLastToPingRange(idx0Datagrams.getFirst(), idx0Datagrams.getLast());
               }
            }
            try {
               PingRange pingRange = EK60Utils.createSegmentInfo(previousIdx, datagramTypeManager).pingRange();
               if (!pingRange.isEmpty()) {
                  return pingRange;
               }
            } catch (DataException _) {
               // This segment could not be created - the reason why will be displayed later for example in DataConf
            } catch (IOException e) {
               Log.global.log(Level.WARNING, "Error reading index file " + previousIdx, e);
            }
         }

         return null;
      }
   }
}
