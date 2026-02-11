package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

/**
 * {@link DataFormatPlugin} for EK60 data.
 */
public final class EK60DataFormatPlugin extends DataFormatPlugin {
   public static final String RAW_SUFFIX = ".raw";
   public static final String IDX_SUFFIX = ".idx";
   public static final String BOT_SUFFIX = ".bot";
   static final String XYZ_SUFFIX = ".xyz";

   private final DatagramTypeManager datagramTypeManager;

   public EK60DataFormatPlugin(Name name, DatagramTypeManager datagramTypeManager) {
      super(name, "EK60 raw file", List.of(RAW_SUFFIX));

      this.datagramTypeManager = datagramTypeManager;
   }

   @Override
   public @Nullable SegmentHandle createSegmentHandle(Path file) {
      String path = file.toString();
      if (!(path.endsWith(RAW_SUFFIX) || path.endsWith(IDX_SUFFIX) || path.endsWith(BOT_SUFFIX))) {
         return null;
      }
      return new EK60SegmentHandle(new EK60FileSet(file), datagramTypeManager);
   }

   @Override
   public List<SegmentHandle> createSegmentHandles(Set<Path> files, AsyncHandle asyncHandle) {
      NavigableSet<String> xyzFiles = XyzUtils.toXyzFiles(files);

      List<SegmentHandle> segmentHandles = new ArrayList<>();
      for (Path file : files) {
         if (asyncHandle.isCancelled()) {
            return List.of();
         }
         String path = file.toString();
         if (path.endsWith(RAW_SUFFIX)) {
            SegmentHandle segmentHandle = new EK60SegmentHandle(new EK60FileSet(file, xyzFiles), datagramTypeManager);
            segmentHandles.add(segmentHandle);
         }
      }
      return segmentHandles;
   }
}
