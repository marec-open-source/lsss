package no.imr.korona.data.formats.missing;

import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentDataPingReader;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;

import java.nio.file.Path;
import java.util.List;

public final class MissingSegmentHandle extends SegmentHandle {
   private final Path file;
   private final SegmentInfo segmentInfo;
   private final PingConfiguration pingConfiguration;

   public MissingSegmentHandle(PingConfiguration pingConfiguration, PingRange pingRange) {
      super("MissingPings_" + pingRange.begin().getPingNumber() + "-" + (pingRange.end().getPingNumber() - 1));

      file = Path.of(getBaseName());
      segmentInfo = new SegmentInfo(pingConfiguration.getRawFileConfiguration(), pingRange);
      this.pingConfiguration = pingConfiguration;
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
      return segmentInfo;
   }

   @Override
   public SegmentData createSegmentData(NoticeHandler noticeHandler, AsyncHandle asyncHandle) {
      return new MissingSegmentData(pingConfiguration, segmentInfo.pingRange());
   }

   @Override
   public PingReader createPingReader() {
      return new SegmentDataPingReader(new MissingSegmentData(pingConfiguration, segmentInfo.pingRange()), file);
   }
}
