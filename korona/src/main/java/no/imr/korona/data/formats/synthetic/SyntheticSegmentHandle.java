package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * {@link SegmentHandle} for synthetic data.
 */
final class SyntheticSegmentHandle extends SegmentHandle {
   private final Path file;
   private final SyntheticDataFile syntheticDataFile;

   SyntheticSegmentHandle(Path file, SyntheticDataFile syntheticDataFile) {
      super(FileUtils.baseName(file));

      this.file = file;
      this.syntheticDataFile = syntheticDataFile;
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
   public SegmentInfo createSegmentInfo() {
      return new SegmentInfo(syntheticDataFile.getRawFileConfiguration(), syntheticDataFile.toPingRange());
   }

   @Override
   public SegmentData createSegmentData(NoticeHandler noticeHandler, AsyncHandle asyncHandle) {
      return new SyntheticSegmentData(syntheticDataFile);
   }

   @Override
   public PingReader createPingReader() {
      return new SyntheticPingReader(file, syntheticDataFile);
   }
}
