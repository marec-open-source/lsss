package no.imr.korona.viewer.variables.adcp;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.netcdf.NetcdfUtils;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.RangeMap;
import org.jspecify.annotations.Nullable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

public final class AdcpData {
   private static final LoadingCache<Path, AdcpData> DIR_TO_ADCP_DATA = CacheBuilder.newBuilder()
         .maximumSize(1)
         .<Path, AdcpData>removalListener(notification -> {
            AdcpData adcpData = notification.getValue();
            if (adcpData != null) {
               adcpData.close();
            }
         })
         .build(CacheLoader.from(AdcpData::forDirectory));

   private final List<AdcpFile> adcpFiles;
   private final RangeMap<Long, AdcpFile> netcdfTimeToAdcpFile = new ArrayRangeMap<>();

   private AdcpData(List<AdcpFile> adcpFiles) {
      this.adcpFiles = adcpFiles;
      for (AdcpFile adcpFile : adcpFiles) {
         long[] pingTimes = adcpFile.getPingTime();
         netcdfTimeToAdcpFile.put(pingTimes[0], pingTimes[pingTimes.length - 1] + 1, adcpFile);
      }
   }

   private static AdcpData forDirectory(Path dir) {
      List<Path> files;
      try {
         files = FileUtils.listFiles(dir, FilePredicates.endsWith(".nc"));
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error listing files in " + dir, e);
         return new AdcpData(List.of());
      }
      List<AdcpFile> adcpFiles = files.stream()
            .<AdcpFile>mapMulti((file, consumer) -> {
               try {
                  AdcpFile adcpFile = AdcpFile.open(file);
                  if (adcpFile == null) {
                     // Does not contain ADCP data.
                     return;
                  }
                  if (adcpFile.getPingTime().length == 0) {
                     Log.global.log(Level.WARNING, "No pings in " + file);
                     return;
                  }
                  consumer.accept(adcpFile);
               } catch (Exception e) {
                  Log.global.log(Level.WARNING, "Error opening " + file, e);
               }
            })
            .toList();
      return new AdcpData(adcpFiles);
   }

   private void close() {
      for (AdcpFile adcpFile : adcpFiles) {
         adcpFile.close();
      }
   }

   static @Nullable AdcpLookup lookup(String variablePath, Ping ping) {
      Path dir = ping.getPingConfiguration().getRawFileConfiguration().getDataFile().getParent();
      AdcpData adcpData = DIR_TO_ADCP_DATA.getUnchecked(dir);
      long netcdfTime = NetcdfUtils.instantToNetcdfTime(ping.getInstant());
      AdcpFile adcpFile = adcpData.netcdfTimeToAdcpFile.get(netcdfTime);
      if (adcpFile == null) {
         return null;
      }
      Variable variable = adcpFile.pathToVariable(variablePath);
      if (variable == null) {
         return null;
      }
      int timeIndex = adcpFile.netcdfTimeToIndex(netcdfTime);
      return new AdcpLookup(adcpFile, variable, timeIndex);
   }

   public static double readDouble(String variablePath, Ping ping, int beamIndex) {
      AdcpLookup adcpLookup = lookup(variablePath, ping);
      if (adcpLookup == null) {
         return Double.NaN;
      }
      try {
         synchronized (adcpLookup.adcpFile().dataset) {
            return beamIndex < 0
                  ? NetcdfUtils.readDouble(adcpLookup.variable(), adcpLookup.timeIndex())
                  : NetcdfUtils.readDouble(adcpLookup.variable(), adcpLookup.timeIndex(), beamIndex);
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error reading " + adcpLookup.variable().getFullName()
               + " from " + adcpLookup.adcpFile().dataset.getLocation(), e);
         return Double.NaN;
      }
   }
}
