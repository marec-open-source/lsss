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
   private final SyntheticDataFile syntheticDataFile;

   SyntheticSegmentHandle(SyntheticDataFile syntheticDataFile) {
      super(FileUtils.baseName(syntheticDataFile.getFile()));

      this.syntheticDataFile = syntheticDataFile;
   }

   @Override
   public String getDisplayName() {
      return syntheticDataFile.getFile().getFileName().toString();
   }

   @Override
   public Path getMainFile() {
      return syntheticDataFile.getFile();
   }

   @Override
   public List<Path> getFiles() {
      return List.of(syntheticDataFile.getFile());
   }

   @Override
   public SegmentInfo createSegmentInfo() {
      return SyntheticSegment.createSegmentInfo(syntheticDataFile.getSyntheticData());
   }

   @Override
   public SegmentData createSegmentData(NoticeHandler noticeHandler, AsyncHandle asyncHandle) {
      return new SyntheticSegmentData(syntheticDataFile.getSyntheticData());
   }

   @Override
   public PingReader createPingReader() {
      return new SyntheticPingReader(syntheticDataFile);
   }
}
