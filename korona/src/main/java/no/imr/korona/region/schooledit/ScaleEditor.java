package no.imr.korona.region.schooledit;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.mask.MaskOutlineTracer;
import no.imr.korona.region.School;
import no.imr.korona.region.SchoolBoundaryObject;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.range.FloatRangeSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.function.Consumer;

public final class ScaleEditor extends SchoolEditor {
   private final List<SchoolBoundaryObject> originalBoundaries;
   private final PingContainer pingContainer;
   private final EchogramPoint grabPoint;
   private final EchogramZSettings zSettings;
   private final CoalescingExecutor maskComputer;
   private final ScaleMaskComputation maskComputationInfo;
   private Set<List<EchogramPoint>> editedBoundary = new HashSet<>();

   public ScaleEditor(School school, Consumer<School> editConfirm, EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      super(school, editConfirm);

      pingContainer = pingSettings.getPingContainer();
      originalBoundaries = school.getBoundaryObjects();
      grabPoint = point;
      this.zSettings = zSettings;
      maskComputer = new CoalescingExecutor(getRegionManager().getRegionConfiguration().getBackgroundExecutor());
      maskComputationInfo = new ScaleMaskComputation(pingSettings, zSettings,
            getOriginalSchoolMaskRepresentation().getSchoolMask(), false);
   }

   @Override
   public List<List<EchogramPoint>> getSortedEditPoints() {
      List<List<EchogramPoint>> list = new ArrayList<>();
      for (List<EchogramPoint> echogramPoints : editedBoundary) {
         list.addAll(SchoolEditUtils.leftToRightLists(echogramPoints));
         // Close the boundary.
         list.addAll(SchoolEditUtils.leftToRightLists(List.of(echogramPoints.getLast(), echogramPoints.getFirst())));
      }
      return list;
   }

   @Override
   public List<List<EchogramPoint>> getSortedToBeRemovedPoints() {
      List<List<EchogramPoint>> list = new ArrayList<>();
      for (SchoolBoundaryObject boundary : originalBoundaries) {
         List<EchogramPoint> echogramPoints = boundary.getBoundary();
         list.addAll(SchoolEditUtils.leftToRightLists(echogramPoints));
         // Close the boundary.
         list.addAll(SchoolEditUtils.leftToRightLists(List.of(echogramPoints.getLast(), echogramPoints.getFirst())));
      }
      return list;
   }

   private void computeMask(float dz) {
      NavigableMap<PingIndex, FloatRangeSet> schoolMask = maskComputationInfo.computeMask(dz);
      editedBoundary = MaskOutlineTracer.createBoundary(schoolMask, pingContainer);
      setEditedSchoolMaskRepresentation(schoolMask);
   }

   @Override
   public void edit(EchogramPoint fromPoint, EchogramPoint toPoint) {
      float grabZ = zSettings.depthToZ(grabPoint.depth(), grabPoint.pingIndex());
      float currentZ = zSettings.depthToZ(toPoint.depth(), toPoint.pingIndex());
      float dz = grabZ - currentZ;

      maskComputer.execute(createManagedCoalescingRunnable(() -> {
         computeMask(dz);
      }));
   }
}
