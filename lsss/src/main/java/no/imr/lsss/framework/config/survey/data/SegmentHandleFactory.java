package no.imr.lsss.framework.config.survey.data;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;

public interface SegmentHandleFactory {
   List<SegmentHandle> createSegmentHandles(Collection<FileInfo> fileInfos, AsyncHandle asyncHandle);

   default SegmentHandlesAndAttributes createSegmentHandlesAndAttributes(Path dir, AsyncHandle asyncHandle) throws IOException {
      List<FileInfo> fileInfos = FileUtils.listFilesWithAttributes(dir, asyncHandle);
      if (asyncHandle.isCancelled()) {
         return new SegmentHandlesAndAttributes(SegmentHandlesAndAttributes.DirListingStatus.CANCELLED);
      }
      List<SegmentHandle> segmentHandles = createSegmentHandles(fileInfos, asyncHandle);
      if (asyncHandle.isCancelled()) {
         return new SegmentHandlesAndAttributes(SegmentHandlesAndAttributes.DirListingStatus.CANCELLED);
      }
      if (segmentHandles.isEmpty()) {
         BasicFileAttributes attributes = FileUtils.readAttributesIfExists(dir);
         if (attributes == null) {
            return new SegmentHandlesAndAttributes(SegmentHandlesAndAttributes.DirListingStatus.NOT_EXISTING);
         } else if (!attributes.isDirectory()) {
            return new SegmentHandlesAndAttributes(SegmentHandlesAndAttributes.DirListingStatus.NOT_DIRECTORY);
         }
      }
      return new SegmentHandlesAndAttributes(segmentHandles, FileUtils.fileInfosToMap(fileInfos));
   }
}
