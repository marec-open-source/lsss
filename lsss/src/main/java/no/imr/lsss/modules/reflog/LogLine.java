package no.imr.lsss.modules.reflog;

import java.time.Instant;
import java.util.List;

public record LogLine(
      Instant time,
      ActivityType activityType,
      boolean start,
      String stationType,
      String localStationNumber,
      List<LogLineField> fields,
      List<String> fieldValues
) {
   public LogLine {
      if (fields.size() != fieldValues.size()) {
         throw new IllegalArgumentException(fields.size() + " != " + fieldValues.size());
      }
   }
}
