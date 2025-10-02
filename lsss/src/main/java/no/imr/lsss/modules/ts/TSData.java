package no.imr.lsss.modules.ts;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

public record TSData(
      float sv,
      float alongshipAngle,
      float athwartshipAngle,
      float tsc,
      float tsu,
      float depth,
      float range,
      long ntDate,
      @Nullable GeoPoint geoPos
) implements BaseTsData {
}
