package no.imr.lsss.modules.ctd;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

record CTDData(
      @Nullable Path file,
      Instant time,
      String stationNumber,
      GeoPoint geographicalPosition,
      int depthColumn,
      List<String> columnNames,
      List<float[]> rows
) {
}
