package no.imr.korona.region.schooledit;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.mask.MaskUtils;
import no.imr.korona.region.School;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.range.FloatRangeSet;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class MoveEditor extends SchoolEditor {
   private final EchogramPoint startPoint;
   private final List<OutlineMoveSettings> outlineMoveSettings;
   private final CoalescingExecutor backgroundMaskComputer;
   private final EchogramPingSettings pingSettings;
   private final EchogramZSettings zSettings;

   public MoveEditor(School school, Consumer<School> editConfirm, EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      super(school, editConfirm);

      startPoint = point;
      outlineMoveSettings = school.getBoundaryObjects().stream()
            .map(OutlineMoveSettings::new)
            .toList();
      backgroundMaskComputer = new CoalescingExecutor(getRegionManager().getRegionConfiguration().getBackgroundExecutor());
      this.pingSettings = pingSettings;
      this.zSettings = zSettings;
   }

   @Override
   public void edit(EchogramPoint fromPoint, EchogramPoint toPoint) {
      PingRange pingRange = PingRange.EMPTY_RANGE;
      for (OutlineMoveSettings outlineMoveSetting : outlineMoveSettings) {
         pingRange = pingRange.union(outlineMoveSetting.move(startPoint, toPoint, pingSettings, zSettings));
      }
      backgroundMaskComputer.execute(createManagedCoalescingRunnable(this::computeMask));
      notifySchoolMaskUpdated(pingRange);
   }

   private void computeMask() {
      PingContainer pingContainer = getRegionManager().getPingContainer();
      DepthTransform depthTransform = zSettings.getDepthTransform();
      NavigableMap<PingIndex, FloatRangeSet> schoolMask = new TreeMap<>();
      for (OutlineMoveSettings outlineMoveSetting : outlineMoveSettings) {
         NavigableMap<PingIndex, FloatRangeSet> mask = MaskUtils.incompleteBoundaryToMask(outlineMoveSetting.getMovedBoundary(), pingContainer, depthTransform);
         MaskUtils.xorAccumulate(schoolMask, mask);
      }
      setEditedSchoolMaskRepresentation(schoolMask);
   }

   @Override
   public List<List<EchogramPoint>> getSortedEditPoints() {
      return outlineMoveSettings.stream()
            .flatMap(outlineMoveSettings -> outlineMoveSettings.getSortedEditPoints().stream())
            .toList();
   }

   @Override
   public List<List<EchogramPoint>> getSortedToBeRemovedPoints() {
      return outlineMoveSettings.stream()
            .flatMap(moveSettings -> moveSettings.getSortedToBeRemovedPoints().stream())
            .toList();
   }
}
