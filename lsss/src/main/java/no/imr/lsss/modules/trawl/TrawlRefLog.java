package no.imr.lsss.modules.trawl;

import no.imr.lsss.modules.reflog.ActivityType;
import no.imr.lsss.modules.reflog.LoaderRefCsv;
import no.imr.lsss.modules.reflog.LogLine;
import no.imr.lsss.modules.reflog.LogLineField;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

final class TrawlRefLog {
   private static final List<LogLineField> FIELDS = List.of(
         new LogLineField("Loc.St.no", ""),
         new LogLineField("Log", ""),
         new LogLineField("Latitude", ""),
         new LogLineField("Longitude", ""),
         new LogLineField("Serial no.", "")
   );

   private TrawlRefLog() {
   }

   static List<LogLine> createLogLines(List<FishStation> stations, Set<String> skipStationNumbers) {
      return stations.stream()
            .flatMap(station -> {
               String stationNumber = Integer.toString(station.stationNumber);
               if (skipStationNumbers.contains(stationNumber)) {
                  return Stream.of();
               }
               return Stream.of(
                     createLogLine(station, stationNumber, station.startTime, true),
                     createLogLine(station, stationNumber, station.stopTime, false)
               );
            })
            .filter(Objects::nonNull)
            .toList();
   }

   private static @Nullable LogLine createLogLine(FishStation station, String stationNumber, @Nullable Instant time, boolean start) {
      if (time == null) {
         return null;
      }
      String stationType = station.toolName + " " + (start ? "start" : "stop") + " (from trawl file)";
      List<String> fieldValues = List.of(
            stationNumber,
            Utils.format("%.3f", station.logStart),
            Utils.format("%.6f", station.latitude),
            Utils.format("%.6f", station.longitude),
            Integer.toString(station.serialNumber)
      );
      ActivityType activityType = LoaderRefCsv.stationTypeToActivityType(stationType);
      return new LogLine(time.toEpochMilli(), activityType, start,
            stationType, stationNumber,
            FIELDS, fieldValues);
   }
}
