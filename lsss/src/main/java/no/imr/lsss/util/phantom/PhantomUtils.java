package no.imr.lsss.util.phantom;

import no.imr.korona.data.track.SegmentHandle;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class PhantomUtils {
   private PhantomUtils() {
   }

   public static NavigableMap<SegmentHandle, SegmentHandle> getRawToPhantom(List<SegmentHandle> phantomSegmentHandles, List<SegmentHandle> rawSegmentHandles) {
      NavigableMap<String, SegmentHandle> phantomNameToSegmentHandle = new TreeMap<>();
      for (SegmentHandle phantomSegmentHandle : phantomSegmentHandles) {
         phantomNameToSegmentHandle.put(phantomSegmentHandle.getBaseName(), phantomSegmentHandle);
      }

      NavigableMap<SegmentHandle, SegmentHandle> rawToPhantom = new TreeMap<>();
      for (SegmentHandle rawSegmentHandle : rawSegmentHandles) {
         String baseName = rawSegmentHandle.getBaseName();
         Map.Entry<String, SegmentHandle> entry = phantomNameToSegmentHandle.ceilingEntry(baseName);
         if (entry != null && entry.getValue().getBaseName().startsWith(baseName)) {
            rawToPhantom.put(rawSegmentHandle, entry.getValue());
         }
      }
      return rawToPhantom;
   }
}
