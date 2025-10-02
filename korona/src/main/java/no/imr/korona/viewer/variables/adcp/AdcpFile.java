package no.imr.korona.viewer.variables.adcp;

import no.imr.tools.logging.Log;
import no.imr.tools.netcdf.NetcdfUtils;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

final class AdcpFile {
   final NetcdfDataset dataset;
   private final Group adcp;
   private final long[] pingTime;
   private final float[] depthFirstSampleCenter;
   private final float[] verticalSampleInterval;
   private final Map<String, Optional<Variable>> pathToVariableMap = new ConcurrentHashMap<>();

   private AdcpFile(NetcdfDataset dataset, Group beamGroup, Group adcp) throws IOException {
      this.dataset = dataset;
      this.adcp = adcp;
      pingTime = NetcdfUtils.readUnsignedLongArray(NetcdfUtils.findVariable(beamGroup, "ping_time"));
      depthFirstSampleCenter = NetcdfUtils.readFloatArray(NetcdfUtils.findVariable(adcp, "depth_first_sample_center"));
      verticalSampleInterval = NetcdfUtils.readFloatArray(NetcdfUtils.findVariable(adcp, "vertical_sample_interval"));
   }

   @Nullable Variable pathToVariable(String path) {
      return pathToVariableMap
            .computeIfAbsent(path, k -> Optional.ofNullable(NetcdfUtils.pathToVariable(adcp, k)))
            .orElse(null);
   }

   static @Nullable AdcpFile open(Path file) throws IOException {
      NetcdfDataset dataset = NetcdfDatasets.openDataset(file.toString(), false, null);
      try {
         Group sonar = NetcdfUtils.findGroup(dataset.getRootGroup(), "Sonar");
         for (Group beamGroup : sonar.getGroups()) {
            Group adcp = beamGroup.findGroupLocal("ADCP");
            if (adcp != null) {
               return new AdcpFile(dataset, beamGroup, adcp);
            }
         }
      } catch (Exception e) {
         try {
            dataset.close();
         } catch (IOException suppressed) {
            e.addSuppressed(suppressed);
         }
         throw e;
      }
      dataset.close();
      return null;
   }

   void close() {
      try {
         dataset.close();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error closing " + dataset.getLocation(), e);
      }
   }

   long[] getPingTime() {
      return pingTime;
   }

   int netcdfTimeToIndex(long netcdfTime) {
      int i = Arrays.binarySearch(pingTime, netcdfTime);
      if (i < 0) {
         i = -2 - i; // = -(i + 1) - 1 = insertion point - 1
         if (i < 0) {
            i = 0;
         }
      }
      return i;
   }

   FloatRange depthRange(int timeIndex, int sampleCount) {
      float delta = verticalSampleInterval[timeIndex];
      float minDepth = depthFirstSampleCenter[timeIndex] - delta / 2;
      float maxDepth = minDepth + delta * sampleCount;
      return FloatRange.of(minDepth, maxDepth);
   }
}
