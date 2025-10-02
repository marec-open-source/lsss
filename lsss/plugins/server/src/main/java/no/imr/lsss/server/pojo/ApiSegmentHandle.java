package no.imr.lsss.server.pojo;

import no.imr.korona.data.track.SegmentHandle;

public final class ApiSegmentHandle {
   public String file;

   public ApiSegmentHandle(SegmentHandle segmentHandle) {
      file = segmentHandle.getDisplayName();
   }

   @Override
   public String toString() {
      return "ApiSegmentHandle{" +
            "file='" + file + '\'' +
            '}';
   }
}
