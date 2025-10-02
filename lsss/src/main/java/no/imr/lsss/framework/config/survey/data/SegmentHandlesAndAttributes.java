package no.imr.lsss.framework.config.survey.data;

import no.imr.korona.data.track.SegmentHandle;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

public record SegmentHandlesAndAttributes(
      DirListingStatus status,
      List<SegmentHandle> segmentHandles,
      Map<Path, BasicFileAttributes> attributes
) {

   public SegmentHandlesAndAttributes(DirListingStatus status) {
      this(status, List.of(), Map.of());
   }

   public SegmentHandlesAndAttributes(List<SegmentHandle> segmentHandles, Map<Path, BasicFileAttributes> attributes) {
      this(DirListingStatus.OK, segmentHandles, attributes);
   }

   public enum DirListingStatus {
      OK,
      NOT_SPECIFIED,
      NOT_EXISTING,
      NOT_DIRECTORY,
      ERROR,
      CANCELLED
   }
}
