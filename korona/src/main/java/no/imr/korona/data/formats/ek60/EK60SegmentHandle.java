package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * {@link SegmentHandle} for EK60 data.
 */
public final class EK60SegmentHandle extends SegmentHandle {
   private final EK60FileSet ek60FileSet;
   private final DatagramTypeManager datagramTypeManager;

   public EK60SegmentHandle(EK60FileSet ek60FileSet, DatagramTypeManager datagramTypeManager) {
      super(FileUtils.baseName(ek60FileSet.getRaw()));

      this.ek60FileSet = ek60FileSet;
      this.datagramTypeManager = datagramTypeManager;
   }

   @Override
   public String getDisplayName() {
      return ek60FileSet.getRaw().getFileName().toString();
   }

   @Override
   public Path getMainFile() {
      return ek60FileSet.getRaw();
   }

   @Override
   public List<Path> getFiles() {
      return ek60FileSet.getFiles();
   }

   public EK60FileSet getEK60FileSet() {
      return ek60FileSet;
   }

   @Override
   public SegmentInfo createSegmentInfo() throws IOException {
      return EK60Utils.createSegmentInfo(ek60FileSet.getIdx(), datagramTypeManager);
   }

   @Override
   public SegmentData createSegmentData(NoticeHandler noticeHandler, AsyncHandle asyncHandle) throws IOException {
      RawFile rawFile = new RawFile(ek60FileSet.getRaw(), EndOfInputHandler.noWait(), datagramTypeManager);
      IdxFile idxFile = IdxFile.load(ek60FileSet.getIdx(), datagramTypeManager, noticeHandler);
      BotFile botFile = BotFile.load(ek60FileSet.getBot(), idxFile, datagramTypeManager, noticeHandler);
      return new EK60SegmentData(
            rawFile,
            idxFile.idx0Datagrams(), idxFile.otherPingItems(), idxFile.wrapAround(),
            botFile.bot0Datagrams()
      );
   }

   @Override
   public List<? extends PingIndex> loadPingIndexes(NoticeHandler noticeHandler, AsyncHandle asyncHandle) throws IOException {
      IdxFile idxFile = IdxFile.load(ek60FileSet.getIdx(), datagramTypeManager, noticeHandler);
      return idxFile.idx0Datagrams();
   }

   @Override
   public PingReader createPingReader() throws IOException {
      return EK60PingReader.create(ek60FileSet, EndOfInputHandler.noWait(), datagramTypeManager, false);
   }
}
