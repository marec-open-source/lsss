package no.imr.lsss.modules.trawl;

import no.imr.lsss.modules.reflog.ActivityType;
import no.imr.lsss.modules.reflog.LogLine;
import no.imr.lsss.modules.reflog.LogLineField;
import no.imr.tools.Utils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

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
            .<LogLine>mapMulti((station, consumer) -> {
               String stationNumber = Integer.toString(station.stationNumber);
               if (skipStationNumbers.contains(stationNumber)) {
                  return;
               }
               if (station.startTime != null) {
                  consumer.accept(createLogLine(station, stationNumber, station.startTime, true));
               }
               if (station.stopTime != null) {
                  consumer.accept(createLogLine(station, stationNumber, station.stopTime, false));
               }
            })
            .toList();
   }

   private static LogLine createLogLine(FishStation station, String stationNumber, Instant time, boolean start) {
      String stationType = station.toolName + " " + (start ? "start" : "stop") + " (from trawl file)";
      List<String> fieldValues = List.of(
            stationNumber,
            Utils.format("%.3f", station.logStart),
            Utils.format("%.6f", station.latitude),
            Utils.format("%.6f", station.longitude),
            Integer.toString(station.serialNumber)
      );
      ActivityType activityType = ActivityType.PELAGIC_TRAWL;
      return new LogLine(time, activityType, start,
            stationType, stationNumber,
            FIELDS, fieldValues);
   }
}
