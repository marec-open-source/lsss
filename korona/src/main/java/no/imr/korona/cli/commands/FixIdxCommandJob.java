package no.imr.korona.cli.commands;

import no.imr.korona.Korona;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.cli.CliException;
import no.imr.korona.data.formats.ek60.EK60SegmentHandle;
import no.imr.korona.data.formats.ek60.EK60Utils;
import no.imr.korona.data.formats.ek60.IdxFile;
import no.imr.korona.data.formats.ek60.IdxFileAdjuster;
import no.imr.korona.data.ping.PingIndexCorrectionOptions;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.DirectoryListing;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

final class FixIdxCommandJob extends CliCommandJob {
   private final Path source;
   private final Path destination;
   private final Korona korona = new Korona();
   private final AsyncHandle asyncHandle = new AsyncHandle();

   FixIdxCommandJob(Path source, Path destination) {
      this.source = source;
      this.destination = destination;
   }

   @Override
   public void run(InputStream in, PrintStream out) throws Exception {
      Log.global.info("Processing from " + source + " to " + destination);

      DirectoryListing sourceDirListing = DirectoryListing.of(source, asyncHandle);
      List<EK60SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandles(sourceDirListing.map().keySet(), asyncHandle).stream()
            .gather(Utils.allOfType(EK60SegmentHandle.class))
            .toList();
      FileUtils.createDirectories(destination);
      DirectoryListing destinationDirListing = DirectoryListing.of(destination, asyncHandle);

      IdxFileAdjuster idxFileAdjuster = new IdxFileAdjuster();
      Path lastDestinationIdx = null;

      Instant lastCenterTime = null;
      Path lastFile = null;

      for (EK60SegmentHandle segmentHandle : segmentHandles) {
         Path sourceIdx = segmentHandle.getEK60FileSet().getIdx();
         Path destinationIdx = destination.resolve(sourceIdx.getFileName());
         if (destinationDirListing.exists(destinationIdx)) {
            lastDestinationIdx = destinationIdx;
            continue;
         }

         Log.global.info("Creating " + destinationIdx);

         if (lastDestinationIdx != null) {
            IdxFile lastDestinationIdxFile = IdxFile.load(lastDestinationIdx, korona.getDatagramTypeManager(), NoticeHandler.ignore());
            idxFileAdjuster.setLast(lastDestinationIdxFile.idx0Datagrams());
            lastDestinationIdx = null;

            lastCenterTime = getCenterTime(lastDestinationIdxFile);
            lastFile = lastDestinationIdxFile.file();
         }

         IdxFile idxFile;
         if (sourceDirListing.exists(sourceIdx)) {
            idxFile = IdxFile.load(sourceIdx, korona.getDatagramTypeManager(), notice -> {
               Log.global.info(sourceIdx + ": " + notice);
            });
         } else {
            idxFile = EK60Utils.createIdxFile(korona.getDatagramTypeManager(), segmentHandle.getEK60FileSet(),
                  idxFileAdjuster.getLastPingNumber() + 1, asyncHandle, ProgressHandler.ignore(),
                  new PingIndexCorrectionOptions(true, true), false);
            assert idxFile != null;
         }

         if (!idxFile.idx0Datagrams().isEmpty()) {
            Instant centerTime = getCenterTime(idxFile);
            if (lastCenterTime != null && centerTime.isBefore(lastCenterTime)) {
               throw new CliException("Center time decreases from "
                     + lastCenterTime + " in " + lastFile.getFileName()
                     + " to " + centerTime + " in " + sourceIdx.getFileName());
            }
            lastCenterTime = centerTime;
            lastFile = idxFile.file();

            idxFileAdjuster.adjustNext(idxFile);
         }

         FileUtils.replaceFileSafely(destinationIdx, idxFile.toBytes());
      }
   }

   private static Instant getCenterTime(IdxFile idxFile) {
      return idxFile.idx0Datagrams().get(idxFile.idx0Datagrams().size() / 2).getInstant();
   }
}
