package no.imr.lsss.modules.schoolparameter.morphological;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.Map;

public final class DepthParameterCollection implements MorphologicalParameterCollection {
   private static final SchoolParameter MIN_DEPTH = new SchoolParameter(new Name("minDepth", "Min depth"), Unit.METER);
   private static final SchoolParameter MAX_DEPTH = new SchoolParameter(new Name("maxDepth", "Max depth"), Unit.METER);
   private static final SchoolParameter MIN_DIST_BOTTOM = new SchoolParameter(new Name("minDistBottom", "Min dist bottom"), Unit.METER);
   private static final SchoolParameter BOTTOM_DEPTH = new SchoolParameter(new Name("bottomDepth", "Bottom depth"), Unit.METER);

   public DepthParameterCollection() {
   }

   @Override
   public List<SchoolParameter> getParameters() {
      return List.of(
            MIN_DEPTH,
            MAX_DEPTH,
            MIN_DIST_BOTTOM,
            BOTTOM_DEPTH
      );
   }

   @Override
   public Map<String, Float> computeValues(DataFileSet dataFileSet, RegionManager regionManager, School school) {
      float totalMinDepth = Float.POSITIVE_INFINITY;
      float totalMaxDepth = Float.NEGATIVE_INFINITY;
      float minDistBottom = Float.POSITIVE_INFINITY;
      float bottomDepth = Float.NaN;

      for (PingIndex pingIndex : dataFileSet.getPingIndices(school.getPingRange())) {
         FloatRange boundingRange = regionManager.getNonMaskedRegionDepthRanges(school, pingIndex).getBoundingRange();
         if (boundingRange.isEmpty()) {
            continue;
         }
         float minDepth = boundingRange.min();
         if (minDepth < totalMinDepth) {
            totalMinDepth = minDepth;
         }
         float maxDepth = boundingRange.max();
         if (maxDepth > totalMaxDepth) {
            totalMaxDepth = maxDepth;
         }
         float coordinatedDepth = dataFileSet.getCoordinatedDepth(pingIndex);
         if (coordinatedDepth != 0) {
            float distBottom = coordinatedDepth - maxDepth;
            if (distBottom < minDistBottom) {
               minDistBottom = distBottom;
               bottomDepth = coordinatedDepth;
            }
         }
      }

      return Map.of(
            MIN_DEPTH.getPersistentName(), totalMinDepth == Float.POSITIVE_INFINITY ? Float.NaN : totalMinDepth,
            MAX_DEPTH.getPersistentName(), totalMaxDepth == Float.NEGATIVE_INFINITY ? Float.NaN : totalMaxDepth,
            MIN_DIST_BOTTOM.getPersistentName(), minDistBottom == Float.POSITIVE_INFINITY ? Float.NaN : minDistBottom,
            BOTTOM_DEPTH.getPersistentName(), bottomDepth
      );
   }
}
