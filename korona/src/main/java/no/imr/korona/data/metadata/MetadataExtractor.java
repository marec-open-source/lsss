package no.imr.korona.data.metadata;

import no.imr.korona.Korona;
import no.imr.korona.data.metadata.pojo.Metadata;
import no.imr.korona.data.metadata.pojo.MetadataConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.DirectoryListing;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class MetadataExtractor {
   private final Metadata metadata = new Metadata();

   private MetadataExtractor(Path dir, Korona korona, AsyncHandle asyncHandle) throws IOException {
      DirectoryListing directoryListing = DirectoryListing.of(dir, asyncHandle);
      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandles(directoryListing.map().keySet(), asyncHandle);
      Map<MetadataConfiguration, List<String>> configToFile = new LinkedHashMap<>();

      PingIndex lastPingIndex = null;

      for (SegmentHandle segmentHandle : segmentHandles) {
         String fileName = segmentHandle.getMainFile().getFileName().toString();
         NoticeHandler noticeHandler = notice -> Log.global.info(fileName + ": Notice: " + notice);
         try (SegmentData segmentData = segmentHandle.createSegmentData(noticeHandler, asyncHandle)) {

            MetadataConfiguration configuration = new MetadataConfiguration(segmentData.getRawFileConfiguration());
            configToFile.computeIfAbsent(configuration, _ -> new ArrayList<>())
                  .add(fileName);

            List<? extends PingIndex> pingIndices = segmentData.getPingIndices();
            if (pingIndices.isEmpty()) {
               Log.global.warning("No ping indices in " + fileName);
            } else {
               if (lastPingIndex != null) {
                  boolean nonMonotonic = false;
                  Duration dt = lastPingIndex.getInstant().until(pingIndices.getFirst().getInstant());
                  if (!dt.isPositive()) {
                     Log.global.warning("Time is non-increasing for " + fileName + " by " + dt.negated());
                     nonMonotonic = true;
                  }
                  double ds = pingIndices.getFirst().getVesselDistance() - lastPingIndex.getVesselDistance();
                  if (ds < 0) {
                     Log.global.warning("Vessel distance is decreasing for " + fileName + " by " + -ds + " nmi");
                     nonMonotonic = true;
                  }
                  if (nonMonotonic) {
                     metadata.errors.nonMonotonicVariables.add(fileName);
                  }
               }
               lastPingIndex = pingIndices.getLast();
            }

         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error opening " + segmentHandle.getMainFile(), e);
            metadata.errors.unusable.add(fileName);
         }
      }

      metadata.configurations = configToFile.entrySet().stream()
            .map(e -> new Metadata.ConfigurationInstance(e.getKey(), e.getValue()))
            .toList();
   }

   public static Metadata extract(Path dir, Korona korona, AsyncHandle asyncHandle) throws IOException {
      return new MetadataExtractor(dir, korona, asyncHandle).metadata;
   }
}
