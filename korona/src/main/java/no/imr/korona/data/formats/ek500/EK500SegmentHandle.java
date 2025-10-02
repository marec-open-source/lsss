package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentDataPingReader;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * {@link SegmentHandle} for EK500 data.
 */
public final class EK500SegmentHandle extends SegmentHandle {
   private final EK500FileSet ek500FileSet;

   EK500SegmentHandle(EK500FileSet ek500FileSet) {
      super(ek500FileSet.getNSS() + "-" + ek500FileSet.getDateTime());

      this.ek500FileSet = ek500FileSet;
   }

   @Override
   public String getDisplayName() {
      return ek500FileSet.getNSS() + "-*-" + ek500FileSet.getDateTime() + "-*";
   }

   @Override
   public Path getMainFile() {
      return ek500FileSet.getMainFile();
   }

   @Override
   public List<Path> getFiles() {
      return ek500FileSet.getFiles();
   }

   @Override
   public SegmentInfo createSegmentInfo() throws IOException {
      return EK500Utils.createSegmentInfo(ek500FileSet);
   }

   @Override
   public SegmentData createSegmentData(NoticeHandler noticeHandler, AsyncHandle asyncHandle) throws IOException {
      return new EK500SegmentData(ek500FileSet, noticeHandler);
   }

   @Override
   public PingReader createPingReader() throws IOException {
      return new SegmentDataPingReader(new EK500SegmentData(ek500FileSet, NoticeHandler.ignore()), getMainFile());
   }
}
