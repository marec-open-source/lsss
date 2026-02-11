package no.imr.korona.data.formats.netcdf;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link DataFormatPlugin} for NetCDF data.
 */
final class NetcdfDataFormatPlugin extends DataFormatPlugin {
   private static final String NETCDF_SUFFIX = ".nc";

   NetcdfDataFormatPlugin(Name name) {
      super(name, "Gridded NetCDF data file", List.of(NETCDF_SUFFIX));
   }

   @Override
   public @Nullable SegmentHandle createSegmentHandle(Path file) {
      if (!file.toString().endsWith(NETCDF_SUFFIX)) {
         return null;
      }
      return new NetcdfSegmentHandle(file);
   }

   @Override
   public List<SegmentHandle> createSegmentHandles(Set<Path> files, AsyncHandle asyncHandle) {
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
