package no.imr.korona.region.schooledit;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.range.FloatRangeSet;

import java.util.List;
import java.util.NavigableMap;
import java.util.function.Consumer;

public abstract class SchoolEditor {
   private final School school;
   private final Consumer<School> editConfirm;
   private final School.SchoolMaskRepresentation originalSchoolMaskRepresentation;
   private School.SchoolMaskRepresentation editedSchoolMaskRepresentation;
   private final AsyncHandle asyncHandle = new AsyncHandle();

   SchoolEditor(School school, Consumer<School> editConfirm) {
      this.school = school;
      this.editConfirm = editConfirm;
      originalSchoolMaskRepresentation = school.getUneditedSchoolMaskRepresentation();
      editedSchoolMaskRepresentation = originalSchoolMaskRepresentation;
   }

   School getSchool() {
      return school;
   }

   public abstract void edit(EchogramPoint fromPoint, EchogramPoint toPoint);

   public void confirm() {
      asyncHandle.waitUntilFinished();
      editConfirm.accept(school);
   }

   /**
    * Get unedited boundaries.
    *
    * @return unedited boundaries
    */
   public List<List<EchogramPoint>> getUneditedBoundaries() {
      return List.of();
   }

   /**
    * Get the user edited points.
    *
    * @return the user edited points in lists sorted from left to right in ping index numbering
    */
   public abstract List<List<EchogramPoint>> getSortedEditPoints();

   /**
    * Get the points which will be removed by this user editing.
    *
    * @return the points which will be removed by this user editing in lists sorted from left to right in ping index numbering
    */
   public abstract List<List<EchogramPoint>> getSortedToBeRemovedPoints();

   /**
    * Get the points which will remain the same after the user editing.
    *
    * @return the points which will remain the same after the user editing in lists sorted from left to right in ping index numbering
    */
   public List<List<EchogramPoint>> getSortedToBeRemovedComplement() {
      return List.of();
   }

   RegionManager getRegionManager() {
      return school.getRegionManager();
   }

   School.SchoolMaskRepresentation getOriginalSchoolMaskRepresentation() {
      return originalSchoolMaskRepresentation;
   }

   public synchronized School.SchoolMaskRepresentation getEditedSchoolMaskRepresentation() {
      return editedSchoolMaskRepresentation;
   }

   synchronized void setEditedSchoolMaskRepresentation(NavigableMap<PingIndex, FloatRangeSet> schoolMask) {
      if (asyncHandle.isCancelled()) {
         return;
      }
      PingRange oldPingRange = editedSchoolMaskRepresentation.getPingRange();
      editedSchoolMaskRepresentation = new School.SchoolMaskRepresentation(schoolMask, getRegionManager().getPingContainer());
      PingRange newPingRange = editedSchoolMaskRepresentation.getPingRange();
      notifySchoolMaskUpdated(oldPingRange.union(newPingRange));
   }

   void notifySchoolMaskUpdated(PingRange pingRange) {
      school.notifySchoolMaskUpdated(pingRange);
   }

   public synchronized void cancel() {
      asyncHandle.cancel();
   }

   Runnable createManagedCoalescingRunnable(Runnable runnable) {
      // Cannot use asyncHandle.createManagedRunnable directly since CoalescingExecute might not execute all jobs.
      return () -> asyncHandle.createManagedRunnable(runnable).run();
   }

   public void switchPointsToKeep() {
   }
}
