package no.imr.korona.viewer;

import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Resamples echogram data to view range settings and window size.
 */
public final class DepthRangeChooser {
   private List<DepthRangeList> depthRangeLists = List.of();
   private DepthRangeMode depthRangeMode = DepthRangeMode.AUTO;
   private FloatRange viewDepthRange = FloatRange.EMPTY_RANGE;
   private FloatRange maxDepthRange = FloatRange.EMPTY_RANGE;

   public DepthRangeChooser() {
   }

   public void reset(int channelCount) {
      depthRangeLists = IntStream.range(0, channelCount)
            .mapToObj(__ -> new DepthRangeList())
            .toList();
   }

   public void computeViewRange() {
      maxDepthRange = computeMaxRange();

      viewDepthRange = switch (depthRangeMode) {
         case MAX -> maxDepthRange;
         case AUTO -> computeAutoRange();
         case MANUAL -> viewDepthRange; // Keep view range.
      };
   }

   public DepthRangeMode getDepthRangeMode() {
      return depthRangeMode;
   }

   public void setDepthRangeMode(DepthRangeMode depthRangeMode) {
      this.depthRangeMode = depthRangeMode;
   }

   public void setMinFixedDepth(float minDepth) {
      if (depthRangeMode != DepthRangeMode.MANUAL) {
         return;
      }

      float maxDepth;
      if (viewDepthRange.isEmpty()) {
         maxDepth = minDepth;
      } else {
         maxDepth = viewDepthRange.max();
      }

      maxDepth = Math.max(maxDepth, minDepth + 1);

      setFixedDepthRange(FloatRange.of(minDepth, maxDepth));
   }

   public void setMaxFixedDepth(float maxDepth) {
      if (depthRangeMode != DepthRangeMode.MANUAL) {
         return;
      }

      float minDepth;
      if (viewDepthRange.isEmpty()) {
         minDepth = maxDepth;
      } else {
         minDepth = viewDepthRange.min();
      }

      minDepth = Math.min(minDepth, maxDepth - 1);

      setFixedDepthRange(FloatRange.of(minDepth, maxDepth));
   }

   public void setFixedDepthRange(FloatRange depthRange) {
      if (depthRangeMode == DepthRangeMode.MANUAL) {
         viewDepthRange = depthRange;
      }
   }

   private FloatRange computeAutoRange() {
      FloatRange depthRange = FloatRange.EMPTY_RANGE;
      for (DepthRangeList depthRangeList : depthRangeLists) {
         FloatRange range = depthRangeList.getAutoRange();
         depthRange = depthRange.union(range);
      }
      return depthRange;
   }

   private FloatRange computeMaxRange() {
      FloatRange depthRange = FloatRange.EMPTY_RANGE;
      for (DepthRangeList depthRangeList : depthRangeLists) {
         FloatRange range = depthRangeList.getMaxRange();
         depthRange = depthRange.union(range);
      }
      return depthRange;
   }

   public FloatRange getMaxDepthRange() {
      return maxDepthRange;
   }

   public FloatRange getViewDepthRange() {
      return viewDepthRange;
   }

   List<DepthRangeList> getDepthRangeLists() {
      return depthRangeLists;
   }

   public void addDepthRange(int channelIndex, FloatRange depthRange) {
      DepthRangeList depthRangeList = depthRangeLists.get(channelIndex);
      depthRangeList.addDepthRange(depthRange);
   }
}
