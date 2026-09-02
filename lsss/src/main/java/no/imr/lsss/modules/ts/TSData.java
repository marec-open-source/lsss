package no.imr.lsss.modules.ts;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record TSData(
      float sv,
      float alongshipAngle,
      float athwartshipAngle,
      float tsc,
      float tsu,
      float depth,
      float range,
      Instant instant,
      @Nullable GeoPoint geoPos
) implements BaseTsData {
}
