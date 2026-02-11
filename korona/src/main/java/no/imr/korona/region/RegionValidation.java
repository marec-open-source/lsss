package no.imr.korona.region;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.mask.MaskUtils;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RegionValidation {
   private RegionValidation() {
   }

   public static void checkLayers(LayerManager layerManager) {
      String error = layerManager.checkForError();
      if (error != null) {
         throw new IllegalStateException(error);
      }
      PingRange totalRange = layerManager.getPingContainer().getTotalRange();
      RangeSet<PingIndex> union = new ArrayRangeSet<>();
      for (Layer layer : layerManager.getLayers()) {
         union.add(layer.getPingRange());

         for (PingIndex pingIndex : layerManager.getPingContainer().getPingIndices(layer.getPingRange())) {
            CurveBoundary upperBoundary = layer.findUpperBoundary(pingIndex);
            CurveBoundary lowerBoundary = layer.findLowerBoundary(pingIndex);
            if (upperBoundary == null || lowerBoundary == null) {
               throw new IllegalStateException();
            }

            // Check the range of a potential new vertical boundary.
            boolean upperIsConnector = !upperBoundary.getPingRange().containsExcludingBegin(pingIndex);
            boolean lowerIsConnector = !lowerBoundary.getPingRange().containsExcludingBegin(pingIndex);

            float startDepth = !upperIsConnector ?
                  upperBoundary.getCurve().getDepth(pingIndex) :
                  (upperBoundary.getPingRange().begin().equals(pingIndex) ?
                        upperBoundary.getStartConnector().getDepth() : upperBoundary.getEndConnector().getDepth());
            float endDepth = !lowerIsConnector ?
                  lowerBoundary.getCurve().getDepth(pingIndex) :
                  (lowerBoundary.getPingRange().begin().equals(pingIndex) ?
                        lowerBoundary.getStartConnector().getDepth() : lowerBoundary.getEndConnector().getDepth());

            if (upperIsConnector) {
               endDepth = Math.max(endDepth, startDepth);
            }
            if (lowerIsConnector) {
               startDepth = Math.min(startDepth, endDepth);
            }
            if (endDepth < startDepth) {
               throw new IllegalStateException();
            }
         }

         // Check vertical boundaries.
         for (VerticalBoundary verticalBoundary : layer.getVerticalBoundaries()) {
            LayerConnector startConnector = verticalBoundary.getStartConnector();
            LayerConnector endConnector = verticalBoundary.getEndConnector();
            if (!startConnector.getPingIndex().equals(endConnector.getPingIndex())) {
               throw new IllegalStateException();
            }
            if (startConnector.getDepth() > endConnector.getDepth()) {
               throw new IllegalStateException();
            }

            for (VerticalBoundary boundary : LayerManager.getAllAffectedBoundaries(verticalBoundary)) {
               if (boundary.equals(verticalBoundary)) {
                  continue;
               }
               if (verticalBoundary.isBelow(boundary)) {
                  if (boundary.getMaxDepth() > verticalBoundary.getMinDepth()) {
                     throw new IllegalStateException();
                  }
                  if (boundary.getMinDepth() > verticalBoundary.getMinDepth()) {
                     throw new IllegalStateException();
                  }
               }
               if (verticalBoundary.isAbove(boundary)) {
                  if (boundary.getMaxDepth() < verticalBoundary.getMaxDepth()) {
                     throw new IllegalStateException();
                  }
                  if (boundary.getMinDepth() < verticalBoundary.getMaxDepth()) {
                     throw new IllegalStateException();
                  }
               }
            }

            // Check that every vertical boundary is connected to 2 layers, except if the index is the first or the last.
            int layerCount = verticalBoundary.getLayers().size();
            if (totalRange.containsExcludingBegin(verticalBoundary.getPingIndex())) {
               if (layerCount != 2) {
                  throw new IllegalStateException();
               }
            } else {
               if (layerCount != 1) {
                  throw new IllegalStateException();
               }
            }
         }

         // Check that all connectors connect at least two boundaries of different type.
         for (LayerConnector layerConnector : layer.getConnectors()) {
            int curveBoundaryCount = layerConnector.getCurveBoundaries().size();
            if (curveBoundaryCount < 1 || curveBoundaryCount > 2) {
               throw new IllegalStateException();
            }
            int verticalBoundaryCount = layerConnector.getVerticalBoundaries().size();
            if (verticalBoundaryCount < 1 || verticalBoundaryCount > 2) {
               throw new IllegalStateException();
            }
         }
      }

      List<Range<PingIndex>> unionRanges = union.stream().toList();
      if (unionRanges.size() != 1 || !unionRanges.getFirst().equals(totalRange)) {
         throw new IllegalStateException();
      }
   }

   public static void checkSchools(SchoolManager schoolManager) {
      Set<Integer> objectNumbers = new HashSet<>();
      for (School school : schoolManager.getSchools()) {
         if (school.hasObjectNumber() && !objectNumbers.add(school.getObjectNumber())) {
            throw new IllegalStateException("Duplicate object number: " + school.getObjectNumber());
         }
         if (school.getSchoolMaskRepresentation().isEmpty()) {
            throw new IllegalStateException();
         }
         for (FloatRangeSet rangeSet : school.getSchoolMaskRepresentation().values()) {
            if (rangeSet.isEmpty()) {
               throw new IllegalStateException();
            }
         }
         schoolManager.regionsIntersectingPingRange(school.getPingRange()).forEach(otherSchool -> {
            if (otherSchool != school && MaskUtils.intersects(otherSchool.getSchoolMaskRepresentation(), school.getSchoolMaskRepresentation())) {
               throw new IllegalStateException();
            }
         });
         if (school.getBoundaryObjects().isEmpty()) {
            throw new IllegalStateException();
         }
         for (SchoolBoundaryObject boundaryObject : school.getBoundaryObjects()) {
            List<EchogramPoint> points = boundaryObject.getBoundary();
            if (points.size() < 4) {
               throw new IllegalStateException();
            }
            for (int i = 0; i < points.size(); i++) {
               EchogramPoint p = points.get(i);
               EchogramPoint q = points.get((i + 1) % points.size());
               if (Math.abs(p.pingIndex().getPingNumber() - q.pingIndex().getPingNumber()) > 1) {
                  // Distance in ping direction between successive points is more than 1.
                  throw new IllegalStateException();
               }
            }
         }
      }
   }
}
