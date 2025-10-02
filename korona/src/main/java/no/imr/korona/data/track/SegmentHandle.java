package no.imr.korona.data.track;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.io.LastModifiedAndSize;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

/**
 * A handle to a {@link Segment}.
 */
public abstract class SegmentHandle implements Comparable<SegmentHandle> {
   private final String baseName;

   protected SegmentHandle(String baseName) {
      this.baseName = baseName;
   }

   @Override
   public String toString() {
      return getDisplayName();
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SegmentHandle that
            && getMainFileAsString().equals(that.getMainFileAsString());
   }

   @Override
   public int hashCode() {
      return getMainFileAsString().hashCode();
   }

   @Override
   public int compareTo(SegmentHandle segmentHandle) {
      return getMainFileAsString().compareTo(segmentHandle.getMainFileAsString());
   }

   private String getMainFileAsString() {
      // On Windows Path::{equals, hashCode, compareTo} is case-insensitive.
      // Therefore, use path::toString.
      return getMainFile().toString();
   }

   public abstract String getDisplayName();

   public String getBaseName() {
      return baseName;
   }

   public LastModifiedAndSize getLastModifiedAndSize(Map<Path, BasicFileAttributes> fileToAttribute, AsyncHandle asyncHandle) {
      return getFiles().stream()
            .map(file -> {
               BasicFileAttributes attributes = fileToAttribute.get(file);
               if (attributes == null) {
                  return LastModifiedAndSize.ZERO;
               }
               return FileUtils.getRecursiveLastModifiedAndSizeOr0(new FileInfo(file, attributes), asyncHandle);
            })
            .reduce(LastModifiedAndSize.ZERO, LastModifiedAndSize::combine);
   }

   public abstract Path getMainFile();

   public abstract List<Path> getFiles();

   public abstract SegmentInfo createSegmentInfo() throws IOException;

   public abstract SegmentData createSegmentData(NoticeHandler noticeHandler, AsyncHandle asyncHandle) throws IOException;

   public List<? extends PingIndex> loadPingIndexes(NoticeHandler noticeHandler, AsyncHandle asyncHandle) throws IOException {
      try (SegmentData segmentData = createSegmentData(noticeHandler, asyncHandle)) {
         return segmentData.getPingIndices();
      }
   }

   public abstract PingReader createPingReader() throws IOException;
}
