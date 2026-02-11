package no.imr.korona.data.formats.netcdf;

import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentDataPingReader;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * {@link SegmentHandle} for synthetic data.
 */
final class NetcdfSegmentHandle extends SegmentHandle {
   private final Path file;

   NetcdfSegmentHandle(Path file) {
      super(FileUtils.baseName(file));

      this.file = file;
   }

   @Override
   public String getDisplayName() {
      return file.getFileName().toString();
   }

   @Override
   public Path getMainFile() {
      return file;
   }

   @Override
   public List<Path> getFiles() {
      return List.of(file);
   }

   @Override
   public SegmentInfo createSegmentInfo() throws IOException {
      try (NetcdfFileData netcdfFileData = new NetcdfFileData(file)) {
         try {
            return netcdfFileData.createSegmentInfo();
         } catch (InvalidRangeException e) {
            throw new IOException(e);
         }
      }
   }

   @Override
   public SegmentData createSegmentData(NoticeHandler noticeHandler, AsyncHandle asyncHandle) throws IOException {
      return new NetcdfSegmentData(file);
   }

   @Override
   public PingReader createPingReader() throws IOException {
      return new SegmentDataPingReader(new NetcdfSegmentData(file), file);
   }
}
