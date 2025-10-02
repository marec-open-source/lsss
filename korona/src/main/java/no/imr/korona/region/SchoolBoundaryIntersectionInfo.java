package no.imr.korona.region;

import no.imr.korona.data.util.geometry.EchogramPoint;

public record SchoolBoundaryIntersectionInfo(
      SchoolBoundaryObject schoolBoundary,
      int startIndex,
      int endIndex,
      EchogramPoint closestPoint,
      double distanceSquared
) {
   public EchogramPoint getStartPoint() {
      return schoolBoundary.getBoundary().get(startIndex);
   }

   public EchogramPoint getEndPoint() {
      return schoolBoundary.getBoundary().get(endIndex);
   }
}
