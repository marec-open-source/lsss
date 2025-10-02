package no.imr.lsss.framework.config.survey.data;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.LSSS;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileInfo;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public final class DefaultSegmentHandleFactory implements SegmentHandleFactory {
   private final LSSS lsss;

   public DefaultSegmentHandleFactory(LSSS lsss) {
      this.lsss = lsss;
   }

   @Override
   public List<SegmentHandle> createSegmentHandles(Collection<FileInfo> fileInfos, AsyncHandle asyncHandle) {
      List<Path> files = fileInfos.stream()
            .map(FileInfo::file)
            .toList();
      return lsss.getKorona().getDataFormatManager().createSegmentHandles(files, asyncHandle);
   }
}
