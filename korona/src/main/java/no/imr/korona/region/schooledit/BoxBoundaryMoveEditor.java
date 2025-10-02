package no.imr.korona.region.schooledit;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramUtils;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.region.School;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeSet;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class BoxBoundaryMoveEditor extends SchoolEditor {
   public enum BoxEditMode {
      TOP_LEFT, TOP, TOP_RIGHT,
      LEFT, RIGHT,
      BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
   }

   private final PingRange pingRange;
   private final FloatRange zRange;
   private final BoxEditMode boxEditMode;
   private final DepthTransform depthTransform;
   private final PingContainer pingContainer;

   private EchogramPoint toPoint;
   private boolean switchPoints;

   private List<List<EchogramPoint>> editPoints = List.of();
   private List<List<EchogramPoint>> keepPoints = List.of();
   private List<List<EchogramPoint>> removePoints = List.of();

   public BoxBoundaryMoveEditor(School school, Consumer<School> editConfirm, EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      super(school, editConfirm);

      pingRange = school.getPingRange();
      zRange = zRange(school, zSettings);
      toPoint = point;
      boxEditMode = pointToMode(point, pingRange, zRange, pingSettings, zSettings);
      depthTransform = zSettings.getDepthTransform();
      pingContainer = getRegionManager().getPingContainer();
   }

   public static FloatRange zRange(School school, EchogramZSettings zSettings) {
      PingIndex pingIndex = school.getPingRange().begin();
      FloatRange depthRange = school.getDepthRanges(pingIndex).getBoundingRange();
      return zSettings.getDepthTransform().depthToZ(depthRange, pingIndex);
   }

   public static BoxEditMode pointToMode(EchogramPoint point, PingRange pingRange, FloatRange zRange, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      float x = pingSettings.pingIndexToX(point.pingIndex());
      float y = zSettings.depthToY(point.depth(), point.pingIndex());

      float xMin = pingSettings.pingIndexToX(pingRange.begin());
      float xMax = pingSettings.pingIndexToX(pingRange.end());

      float yMin = zSettings.zToY(zRange.min());
      float yMax = zSettings.zToY(zRange.max());

      if (y < yMin) {
         if (x < xMin) return BoxEditMode.TOP_LEFT;
         if (x >= xMax) return BoxEditMode.TOP_RIGHT;
      }
      if (y > yMax) {
         if (x < xMin) return BoxEditMode.BOTTOM_LEFT;
         if (x >= xMax) return BoxEditMode.BOTTOM_RIGHT;
      }

      record Candidate(BoxEditMode mode, double distSq) {
      }
      Candidate left = new Candidate(BoxEditMode.LEFT, Line2D.ptSegDistSq(xMin, yMin, xMin, yMax, x, y));
      Candidate right = new Candidate(BoxEditMode.RIGHT, Line2D.ptSegDistSq(xMax, yMin, xMax, yMax, x, y));
      Candidate top = new Candidate(BoxEditMode.TOP, Line2D.ptSegDistSq(xMin, yMin, xMax, yMin, x, y));
      Candidate bottom = new Candidate(BoxEditMode.BOTTOM, Line2D.ptSegDistSq(xMin, yMax, xMax, yMax, x, y));

      Candidate bestHorizontal = left.distSq < right.distSq ? left : right;
      Candidate bestVertical = top.distSq < bottom.distSq ? top : bottom;
      // Choose vertical for equal distance.
      Candidate best = bestHorizontal.distSq < bestVertical.distSq ? bestHorizontal : bestVertical;
      return best.mode;
   }

   @Override
   public void switchPointsToKeep() {
      switchPoints = !switchPoints;
      update();
   }

   @Override
   public List<List<EchogramPoint>> getSortedEditPoints() {
      return editPoints;
   }

   @Override
   public List<List<EchogramPoint>> getSortedToBeRemovedPoints() {
      return removePoints;
   }

   @Override
   public List<List<EchogramPoint>> getSortedToBeRemovedComplement() {
      return keepPoints;
   }

   @Override
   public void edit(EchogramPoint fromPoint, EchogramPoint toPoint) {
      this.toPoint = toPoint;
      update();
   }

   private void update() {
      PingRange editPingRange = switch (boxEditMode) {
         case LEFT, TOP_LEFT, BOTTOM_LEFT -> PingRange.ofUnsorted(toPoint.pingIndex(), switchPoints ? pingRange.begin() : pingRange.end());
         case RIGHT, TOP_RIGHT, BOTTOM_RIGHT -> PingRange.ofUnsorted(toPoint.pingIndex(), switchPoints ? pingRange.end() : pingRange.begin());
         default -> pingRange;
      };
      FloatRange editZRange = switch (boxEditMode) {
         case TOP_LEFT, TOP, TOP_RIGHT -> FloatRange.ofUnsorted(depthTransform.depthToZ(toPoint), switchPoints ? zRange.min() : zRange.max());
         case BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT -> FloatRange.ofUnsorted(depthTransform.depthToZ(toPoint), switchPoints ? zRange.max() : zRange.min());
         default -> zRange;
      };

      Map<Float, RangeSet<PingIndex>> editHorizontal = new HashMap<>();
      editHorizontal.put(editZRange.min(), new ArrayRangeSet<>(editPingRange));
      editHorizontal.put(editZRange.max(), new ArrayRangeSet<>(editPingRange));
      Map<PingIndex, FloatRangeSet> editVertical = new HashMap<>();
      editVertical.put(editPingRange.begin(), FloatRangeSet.of(editZRange));
      editVertical.put(editPingRange.end(), FloatRangeSet.of(editZRange));

      Map<Float, RangeSet<PingIndex>> keepHorizontal = new HashMap<>();
      Map<PingIndex, FloatRangeSet> keepVertical = new HashMap<>();

      Map<Float, RangeSet<PingIndex>> removeHorizontal = new HashMap<>();
      removeHorizontal.put(zRange.min(), new ArrayRangeSet<>(pingRange));
      removeHorizontal.put(zRange.max(), new ArrayRangeSet<>(pingRange));
      Map<PingIndex, FloatRangeSet> removeVertical = new HashMap<>();
      removeVertical.put(pingRange.begin(), FloatRangeSet.of(zRange));
      removeVertical.put(pingRange.end(), FloatRangeSet.of(zRange));

      for (float z : new float[]{editZRange.min(), editZRange.max()}) {
         RangeSet<PingIndex> removeHorizontalAtZ = removeHorizontal.get(z);
         if (removeHorizontalAtZ != null) {
            PingRange intersection = pingRange.intersection(editPingRange);
            keepHorizontal.put(z, new ArrayRangeSet<>(intersection));
            removeHorizontalAtZ.remove(intersection);
            editHorizontal.get(z).remove(intersection);
         }
      }
      for (PingIndex pingIndex : new PingIndex[]{editPingRange.begin(), editPingRange.end()}) {
         FloatRangeSet removeVerticalAtPing = removeVertical.get(pingIndex);
         if (removeVerticalAtPing != null) {
            FloatRange intersection = zRange.intersection(editZRange);
            keepVertical.put(pingIndex, FloatRangeSet.of(intersection));
            removeVertical.put(pingIndex, removeVerticalAtPing.subtract(intersection));
            editVertical.put(pingIndex, editVertical.get(pingIndex).subtract(intersection));
         }
      }

      editPoints = leftToRightLists(editHorizontal, editVertical);
      keepPoints = leftToRightLists(keepHorizontal, keepVertical);
      removePoints = leftToRightLists(removeHorizontal, removeVertical);

      NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();
      if (!editZRange.isEmpty()) {
         for (PingIndex pingIndex : pingContainer.getPingIndices(editPingRange)) {
            FloatRange depthRange = depthTransform.zToDepth(editZRange, pingIndex);
            mask.put(pingIndex, FloatRangeSet.of(depthRange));
         }
      }
      setEditedSchoolMaskRepresentation(mask);
   }

   private List<List<EchogramPoint>> leftToRightLists(Map<Float, RangeSet<PingIndex>> horizontal, Map<PingIndex, FloatRangeSet> vertical) {
      List<List<EchogramPoint>> lists = new ArrayList<>();
      horizontal.forEach((z, pingRanges) -> {
         pingRanges.forEach(pingRange -> {
            lists.add(EchogramUtils.computeLine(depthTransform,
                  new EchogramPoint(pingRange.begin(), depthTransform.zToDepth(z, pingRange.begin())),
                  new EchogramPoint(pingRange.end(), depthTransform.zToDepth(z, pingRange.end())),
                  pingContainer));
         });
      });
      vertical.forEach((pingIndex, zRanges) -> {
         zRanges.getFloatRanges().forEach(zRange -> {
            lists.add(List.of(
                  new EchogramPoint(pingIndex, depthTransform.zToDepth(zRange.min(), pingIndex)),
                  new EchogramPoint(pingIndex, depthTransform.zToDepth(zRange.max(), pingIndex))
            ));
         });
      });
      return lists;
   }
}
