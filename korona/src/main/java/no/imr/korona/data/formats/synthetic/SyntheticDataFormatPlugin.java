package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link DataFormatPlugin} for synthetic data.
 */
public final class SyntheticDataFormatPlugin extends DataFormatPlugin {
   public static final String LSSS_SS_SUFFIX = ".lsss-ss";

   SyntheticDataFormatPlugin(Name name) {
      super(name, "LSSS synthetic survey", List.of(LSSS_SS_SUFFIX));
   }

   @Override
   public @Nullable SegmentHandle createSegmentHandle(Path file) throws IOException {
      if (!file.toString().endsWith(LSSS_SS_SUFFIX)) {
         return null;
      }
      return new SyntheticSegmentHandle(file, SyntheticDataFile.toSyntheticData(file));
   }

   @Override
   public List<SegmentHandle> createSegmentHandles(Set<Path> files, AsyncHandle asyncHandle) throws IOException {
      List<SegmentHandle> segmentHandles = new ArrayList<>();
      for (Path file : files) {
         if (asyncHandle.isCancelled()) {
            return List.of();
         }
         SegmentHandle segmentHandle = createSegmentHandle(file);
         if (segmentHandle != null) {
            segmentHandles.add(segmentHandle);
         }
      }
      return segmentHandles;
   }
}
