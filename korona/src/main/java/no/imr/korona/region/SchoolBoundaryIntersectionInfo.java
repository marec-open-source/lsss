package no.imr.korona.region;

import no.imr.korona.data.util.geometry.EchogramPoint;

public record SchoolBoundaryIntersectionInfo(
      School school,
      SchoolBoundaryObject schoolBoundary,
      int startIndex,
      EchogramPoint closestPoint,
      double distanceSquared
) {
   public EchogramPoint getStartPoint() {
      return schoolBoundary.getBoundary().get(startIndex);
   }

   public EchogramPoint getEndPoint() {
      return schoolBoundary.getBoundary().get((startIndex + 1) % schoolBoundary.getBoundary().size());
   }
}
