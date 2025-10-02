package no.imr.korona.region.schooledit;

import com.google.common.collect.ListMultimap;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramUtils;
import no.imr.korona.data.util.mask.MaskOutlineTracer;
import no.imr.korona.region.School;
import no.imr.korona.region.SchoolBoundaryIntersectionInfo;
import no.imr.korona.region.SchoolBoundaryObject;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.CyclicList;
import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public final class BoundaryDrawEditor extends SchoolEditor {
   private final SchoolBoundaryIntersectionInfo startEditInfo;
   private final SchoolBoundaryObject schoolBoundary;
   private final List<List<EchogramPoint>> uneditedSchoolBoundaries;
   private final PingContainer pingContainer;
   private final EchogramPoint boundaryCenter;
   private final Deque<EchogramPoint> editPoints = new ConcurrentLinkedDeque<>();
   private final EchogramPingSettings pingSettings;
   private final EchogramZSettings zSettings;
   private List<EchogramPoint> connectionPoints = List.of();
   private SchoolBoundaryIntersectionInfo currentEditInfo;
   private boolean currentDrawnBoundaryType = true;
   private boolean keepComplementaryPoints = false;
   private final CoalescingExecutor windingChecker;

   public BoundaryDrawEditor(School school, Consumer<School> editConfirm, EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      super(school, editConfirm);

      windingChecker = new CoalescingExecutor(getRegionManager().getRegionConfiguration().getBackgroundExecutor());
      startEditInfo = school.distanceFrom(point, pingSettings, zSettings);
      schoolBoundary = startEditInfo.schoolBoundary();
      this.pingSettings = pingSettings;
      this.zSettings = zSettings;
      pingContainer = getRegionManager().getPingContainer();
      boundaryCenter = computeCenter(schoolBoundary, pingContainer);
      uneditedSchoolBoundaries = school.getBoundaryObjects().stream()
            .filter(schoolBoundaryObject -> schoolBoundaryObject != schoolBoundary)
            .<List<EchogramPoint>>map(SchoolBoundaryObject::getBoundary)
            .toList();
      currentEditInfo = startEditInfo;
      edit(startEditInfo.closestPoint(), point);
   }

   public List<EchogramPoint> getOriginalBoundary() {
      return schoolBoundary.getBoundary();
   }

   @Override
   public List<List<EchogramPoint>> getUneditedBoundaries() {
      return uneditedSchoolBoundaries;
   }

   private static boolean drawnContainsCenter(List<EchogramPoint> drawnBoundary, EchogramPoint boundaryCenter) {
      ListMultimap<PingIndex, EchogramPoint> pointListMultimap = MaskOutlineTracer.makePingIndexToPointMap(drawnBoundary);
      List<EchogramPoint> list = pointListMultimap.get(boundaryCenter.pingIndex());
      if (list.isEmpty()) {
         return false;
      }
      FloatRangeSet ranges = MaskOutlineTracer.getDepthRanges(list);
      return ranges.contains(boundaryCenter.depth());
   }

   @Override
   public void switchPointsToKeep() {
      keepComplementaryPoints = !keepComplementaryPoints;

      notifySchoolMaskUpdated(getSchool().getPingRange());
   }

   private List<EchogramPoint> getToBeRemovedPoints() {
      return getBetweenPoints(currentDrawnBoundaryType ^ keepComplementaryPoints);
   }

   private List<EchogramPoint> getSchoolBoundaryBetweenComplement() {
      return getBetweenPoints(!currentDrawnBoundaryType ^ keepComplementaryPoints);
   }

   private List<EchogramPoint> getBetweenPoints(boolean defaultDir) {
      List<EchogramPoint> betweenPoints = new ArrayList<>();
      if (startEditInfo.startIndex() <= currentEditInfo.startIndex()) {
         if (defaultDir) {
            betweenPoints.add(editPoints.getFirst());
            betweenPoints.addAll(schoolBoundary.getBoundary().subList(startEditInfo.startIndex(), currentEditInfo.endIndex()));
            betweenPoints.add(connectionPoints.getLast());
         } else { // complementary selection
            betweenPoints.add(connectionPoints.getLast());
            if (currentEditInfo.endIndex() == startEditInfo.endIndex()) {
               betweenPoints.addAll(schoolBoundary.getBoundary().subList(currentEditInfo.endIndex(), startEditInfo.endIndex() - 1));
               betweenPoints.add(schoolBoundary.getBoundary().get(startEditInfo.endIndex() - 1));
            } else {
               betweenPoints.addAll(schoolBoundary.getBoundary().subList(currentEditInfo.endIndex(), startEditInfo.endIndex()));
            }
            betweenPoints.add(editPoints.getFirst());
         }
      } else { // startEditInfo.getStartIndex() > currentEditInfo.getStartIndex()
         if (defaultDir) {
            betweenPoints.add(editPoints.getFirst());
            if (startEditInfo.startIndex() == currentEditInfo.endIndex()) {
               betweenPoints.addAll(schoolBoundary.getBoundary().subList(startEditInfo.startIndex(), currentEditInfo.endIndex() - 1));
               betweenPoints.add(schoolBoundary.getBoundary().get(currentEditInfo.endIndex() - 1));
            } else {
               betweenPoints.addAll(schoolBoundary.getBoundary().subList(startEditInfo.startIndex(), currentEditInfo.endIndex()));
            }
            betweenPoints.add(connectionPoints.getLast());
         } else {
            betweenPoints.add(connectionPoints.getLast());
            betweenPoints.addAll(schoolBoundary.getBoundary().subList(currentEditInfo.startIndex(), startEditInfo.endIndex()));
            betweenPoints.add(editPoints.getFirst());
         }
      }
      return betweenPoints;
   }

   public CyclicList<EchogramPoint> getDrawnBoundary() {
      return getDrawnBoundary(currentDrawnBoundaryType ^ keepComplementaryPoints);
   }

   private CyclicList<EchogramPoint> getDrawnBoundary(boolean defaultDir) {
      CyclicList<EchogramPoint> result = new CyclicList<>();
      if (defaultDir) {
         List<EchogramPoint> reverseBetweenPoints = getBetweenPoints(true);
         Collections.reverse(reverseBetweenPoints);
         result.addAll(editPoints);
         result.addAll(connectionPoints);
         result.addAll(reverseBetweenPoints);
      } else {
         List<EchogramPoint> betweenPoints = getBetweenPoints(false);
         result.addAll(betweenPoints);
         result.addAll(editPoints);
         result.addAll(connectionPoints);
      }
      return result;
   }

   private static EchogramPoint computeCenter(SchoolBoundaryObject schoolBoundary, PingContainer pingContainer) {
      long middlePingNumber = (schoolBoundary.getPingRange().begin().getPingNumber() + schoolBoundary.getPingRange().end().getPingNumber()) / 2;
      PingIndex middlePingIndex = pingContainer.getPingIndex(middlePingNumber);
      ListMultimap<PingIndex, EchogramPoint> pointListMultimap = MaskOutlineTracer.makePingIndexToPointMap(schoolBoundary.getBoundary());
      List<EchogramPoint> pointsForMiddlePingIndex = pointListMultimap.get(middlePingIndex);
      FloatRangeSet ranges = MaskOutlineTracer.getDepthRanges(pointsForMiddlePingIndex);
      FloatRange range = ranges.getFloatRanges().getFirst();
      return new EchogramPoint(middlePingIndex, range.getCenter());
   }

   @Override
   public void edit(EchogramPoint fromPoint, EchogramPoint toPoint) {
      List<EchogramPoint> line = EchogramUtils.computeLine(zSettings.getDepthTransform(), fromPoint, toPoint, pingContainer);
      if (!editPoints.isEmpty() && editPoints.getLast().equals(line.getFirst())) {
         line = line.subList(1, line.size());
      }
      editPoints.addAll(line);
      List<EchogramPoint> newConnectionPoints = new ArrayList<>();
      newConnectionPoints.add(toPoint);
      currentEditInfo = schoolBoundary.getClosestIntersection(toPoint, pingSettings, zSettings);
      EchogramPoint closestConnectionPoint = currentEditInfo.closestPoint();
      newConnectionPoints.addAll(EchogramUtils.computeLine(zSettings.getDepthTransform(), toPoint, closestConnectionPoint, pingContainer));
      newConnectionPoints.add(closestConnectionPoint);
      connectionPoints = newConnectionPoints;

      CyclicList<EchogramPoint> drawnBoundary = getDrawnBoundary();
      windingChecker.execute(createManagedCoalescingRunnable(() -> {
         checkWinding(drawnBoundary);
      }));
      notifySchoolMaskUpdated(PingRange.ofUnsorted(fromPoint.pingIndex(), toPoint.pingIndex()));
   }

   private void checkWinding(List<EchogramPoint> drawnBoundary) {
      if (drawnContainsCenter(drawnBoundary, boundaryCenter)) {
         currentDrawnBoundaryType = !currentDrawnBoundaryType;
      }
   }

   @Override
   public List<List<EchogramPoint>> getSortedEditPoints() {
      List<List<EchogramPoint>> list = new ArrayList<>();
      list.addAll(SchoolEditUtils.leftToRightLists(editPoints));
      list.addAll(SchoolEditUtils.leftToRightLists(connectionPoints));
      return list;
   }

   @Override
   public List<List<EchogramPoint>> getSortedToBeRemovedPoints() {
      return SchoolEditUtils.leftToRightLists(getToBeRemovedPoints());
   }

   @Override
   public List<List<EchogramPoint>> getSortedToBeRemovedComplement() {
      return SchoolEditUtils.leftToRightLists(getSchoolBoundaryBetweenComplement());
   }
}
