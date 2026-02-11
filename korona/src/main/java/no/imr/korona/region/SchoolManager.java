package no.imr.korona.region;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.mask.MaskUtils;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.Pair;
import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class SchoolManager extends BaseRegionManager<School> {
   private static final String XML_SCHOOL_INTERPRETATION = "schoolInterpretation";
   private static final int UNDO_LIMIT = 5;

   private final Set<School> schools = ConcurrentHashMap.newKeySet();
   private final SchoolUndoManager undoManager = new SchoolUndoManager();
   private final ChangeManager undoChangeManager = new ChangeManager();

   public SchoolManager(RegionManager regionManager) {
      super(regionManager);

      undoManager.setLimit(UNDO_LIMIT);
   }

   List<School> addVerticalDivider(PingIndex pingIndex, boolean inheritVisitedStatus) {
      List<School> allNewSchools = new ArrayList<>();
      for (School school : schools) {
         List<School> newSchools = splitSchool(school, pingIndex);
         if (newSchools.isEmpty()) {
            continue;
         }
         if (inheritVisitedStatus) {
            for (School newSchool : newSchools) {
               newSchool.setSelectedAtLeastOnce(school.isSelectedAtLeastOnce());
            }
         }
         allNewSchools.addAll(newSchools);

         //todo conditional removal of edits seems not to work in stress test. Removing all edits for now.
         removeAllUndoEdits();
      }
      return allNewSchools;
   }

   @Nullable School addSchoolNew(EchogramPoint startPoint, EchogramPoint endPoint, DepthTransform depthTransform) {
      PingRange pingRange = PingRange.ofUnsorted(startPoint.pingIndex(), endPoint.pingIndex());
      pingRange = pingRange.intersection(getVisiblePingRange());
      float minZ = Math.min(depthTransform.depthToZ(startPoint), depthTransform.depthToZ(endPoint));
      float maxZ = Math.max(depthTransform.depthToZ(startPoint), depthTransform.depthToZ(endPoint));
      FloatRange zRange = FloatRange.of(minZ, maxZ);
      NavigableMap<PingIndex, FloatRangeSet> schoolMask = MaskUtils.boxMask(pingRange, zRange, depthTransform, getPingContainer());
      return addSchoolNew(schoolMask);
   }

   @Nullable School addSchoolNew(NavigableMap<PingIndex, FloatRangeSet> schoolMask) {
      School school = School.create(getRegionManager(), schoolMask);
      if (school.isEmpty()) {
         return null;
      }
      addEdit(new SchoolAdd(school));
      schools.add(school);
      getRegionManager().notifyRegionBoundaryChanged(school.getPingRange(), school);
      return school;
   }

   private SingleSchoolEdit addSchool(School school) {
      addSchoolWithoutUndoEdit(school);
      getRegionManager().notifyRegionBoundaryChanged(school.getPingRange(), school);
      return new SchoolAdd(school);
   }

   @Nullable School addSchool(List<EchogramPoint> points, DepthTransform depthTransform) {
      NavigableMap<PingIndex, FloatRangeSet> schoolMask = MaskUtils.incompleteBoundaryToMask(points, getPingContainer(), depthTransform);
      School school = School.create(getRegionManager(), schoolMask);
      if (school.getPingRange().isEmpty()) {
         Log.global.info("Creation of school failed (the school may be overlapping existing schools)");
         return null;
      }
      addSchoolWithoutUndoEdit(school);
      addEdit(new SchoolAdd(school));
      getRegionManager().notifyRegionBoundaryChanged(school.getPingRange(), school);
      return school;
   }

   void addSchoolWithoutUndoEdit(School school) {
      schools.add(school);
   }

   PingRange setSchoolMask(School school, NavigableMap<PingIndex, FloatRangeSet> schoolMask) {
      List<SingleSchoolEdit> schoolEdits = new ArrayList<>();
      schoolEdits.add(createUndoCopy(school));
      PingRange editedPingRange = school.getPingRange();
      school.setMask(schoolMask);
      if (school.isEmpty()) {
         schoolEdits.add(deleteSchool(school));
      } else {
         editedPingRange = editedPingRange.union(school.getPingRange());
         getRegionManager().notifyRegionBoundaryChanged(editedPingRange, school);
      }
      addCompositeEdit(schoolEdits);
      return editedPingRange;
   }

   SingleSchoolEdit deleteSchool(School school) {
      deleteSchoolWithoutUndoEdit(school);
      return new SchoolRemove(school);
   }

   void deleteSchoolWithoutUndoEdit(School school) {
      schools.remove(school);
      school.setSelected(false);
      if (getSelectedRegions().remove(school)) {
         getRegionManager().notifyRegionListenersSelectedRegions();
      }
      getRegionManager().notifyRegionListenersRegionDeleted(school);
   }

   @Override
   Set<School> getRegions() {
      return schools;
   }

   public Set<School> getSchools() {
      return schools;
   }

   void removeAllSchools() {
      List<School> deletedSchools = List.copyOf(schools);
      schools.clear();
      getRegionManager().notifyRegionListenersRegionsDeleted(deletedSchools);
   }

   private School mergeSchoolsNew(School school1, School school2) {
      NavigableMap<PingIndex, FloatRangeSet> mergedMask = MaskUtils.add(school1.getSchoolMaskRepresentation(), school2.getSchoolMaskRepresentation());
      School mergedSchool = School.createUnconstrained(getRegionManager(), mergedMask);
      mergedSchool.getInterpretation().copyFrom(school1.getInterpretation());
      mergedSchool.setObjectNumber(school1.getObjectNumber());
      mergedSchool.setParameters(school1.getParameters());
      mergedSchool.setLabels(ImmutableUtils.addAll(school1.getLabels(), school2.getLabels()));
      mergedSchool.setSelectedAtLeastOnce(school1.isSelectedAtLeastOnce() && school2.isSelectedAtLeastOnce());
      return mergedSchool;
   }

   @Nullable School mergeSchools(List<School> schools) {
      if (schools.size() < 2) {
         return null;
      }
      School result = schools.getFirst();
      for (int i = 1; i < schools.size(); i++) {
         School school = schools.get(i);
         result = mergeSchoolsNew(result, school);
      }
      List<SingleSchoolEdit> schoolEdits = new ArrayList<>();
      for (School school : schools) {
         schoolEdits.add(deleteSchool(school));
      }
      schoolEdits.add(addSchool(result));
      addCompositeEdit(schoolEdits);
      getRegionManager().notifyLayersInPingRange(result.getPingRange());
      return result;
   }

   void moveConfirm(School school) {
      List<SingleSchoolEdit> schoolEdits = new ArrayList<>();
      schoolEdits.add(createUndoCopy(school));
      PingRange adjustRange = school.moveConfirm();
      if (school.isEmpty()) {
         schoolEdits.add(deleteSchool(school));
      } else {
         getRegionManager().notifyRegionBoundaryChanged(adjustRange, school);
      }
      addCompositeEdit(schoolEdits);
      getRegionManager().notifyLayersInPingRange(adjustRange);
   }

   void boundaryDrawConfirm(School school) {
      List<SingleSchoolEdit> schoolEdits = new ArrayList<>();
      schoolEdits.add(createUndoCopy(school));
      PingRange adjustRange = school.boundaryDrawConfirm();
      if (school.isEmpty()) {
         schoolEdits.add(deleteSchool(school));
      } else {
         getRegionManager().notifyRegionBoundaryChanged(adjustRange, school);
      }
      addCompositeEdit(schoolEdits);
      getRegionManager().notifyLayersInPingRange(adjustRange);
   }

   void boundarySplitConfirm(School school) {
      List<SingleSchoolEdit> schoolEdits = new ArrayList<>();
      schoolEdits.add(createUndoCopy(school));
      PingRange adjustRange = school.getPingRange();
      List<School> newSchools = school.boundarySplitConfirm();
      for (School newSchool : newSchools) {
         if (!newSchool.isEmpty()) {
            schoolEdits.add(addSchool(newSchool));
            adjustRange = adjustRange.union(newSchool.getPingRange());
         }
      }
      if (school.isEmpty()) {
         schoolEdits.add(deleteSchool(school));
      } else {
         getRegionManager().notifyRegionBoundaryChanged(adjustRange, school);
      }
      getRegionManager().notifyRegionBoundaryChanged(adjustRange, newSchools);
      addCompositeEdit(schoolEdits);
      getRegionManager().notifyLayersInPingRange(adjustRange);
   }

   public void cancelEdit() {
      schools.forEach(School::cancelEdit);
   }

   void boxBoundaryMoveConfirm(School school) {
      List<SingleSchoolEdit> schoolEdits = new ArrayList<>();
      schoolEdits.add(createUndoCopy(school));
      PingRange adjustRange = school.boxBoundaryMoveConfirm();
      if (school.isEmpty()) {
         schoolEdits.add(deleteSchool(school));
      } else {
         getRegionManager().notifyRegionBoundaryChanged(adjustRange, school);
      }
      addCompositeEdit(schoolEdits);
      getRegionManager().notifyLayersInPingRange(adjustRange);
   }

   void scaleConfirm(School school) {
      List<SingleSchoolEdit> schoolEdits = new ArrayList<>();
      schoolEdits.add(createUndoCopy(school));
      PingRange adjustRange = school.scaleConfirm(getVisiblePingRange());
      if (school.isEmpty()) {
         schoolEdits.add(deleteSchool(school));
      } else {
         getRegionManager().notifyRegionBoundaryChanged(adjustRange, school);
      }
      addCompositeEdit(schoolEdits);
      getRegionManager().notifyLayersInPingRange(adjustRange);
   }

   void constrainSchoolsAfterLayerEdit(PingRange pingRange) {
      List<School> changedSchools = regionsIntersectingPingRange(pingRange)
            .filter(School::constrainToLayers)
            .map(school -> {
               if (school.isEmpty()) {
                  getRegionManager().deleteSchoolWithoutUndo(school);
                  return null;
               } else {
                  return school;
               }
            })
            .filter(Objects::nonNull)
            .toList();
      if (!changedSchools.isEmpty()) {
         getRegionManager().notifyRegionBoundaryChanged(pingRange, changedSchools);
      }
   }

   private List<School> splitSchool(School school, PingIndex pingIndex) {
      if (!school.getPingRange().containsExcludingBegin(pingIndex)) {
         return List.of();
      }
      NavigableMap<PingIndex, FloatRangeSet> leftMask = new TreeMap<>();
      NavigableMap<PingIndex, FloatRangeSet> rightMask = new TreeMap<>();
      for (Map.Entry<PingIndex, FloatRangeSet> entry : school.getSchoolMaskRepresentation().entrySet()) {
         PingIndex index = entry.getKey();
         FloatRangeSet floatRangeSet = entry.getValue();
         if (index.compareTo(pingIndex) < 0) {
            leftMask.put(index, floatRangeSet);
         } else {
            rightMask.put(index, floatRangeSet);
         }
      }
      deleteSchool(school);
      List<School> result = new ArrayList<>();
      if (!leftMask.isEmpty()) {
         result.add(createUnconstrainedSchool(school, leftMask));
      }
      if (!rightMask.isEmpty()) {
         result.add(createUnconstrainedSchool(school, rightMask));
      }
      return result;
   }

   private School createUnconstrainedSchool(School oldSchool, NavigableMap<PingIndex, FloatRangeSet> mask) {
      School newSchool = School.createUnconstrained(getRegionManager(), mask);
      newSchool.getInterpretation().copyFrom(oldSchool.getInterpretation());
      newSchool.setLabels(oldSchool.getLabels());
      schools.add(newSchool);
      getRegionManager().notifyRegionBoundaryChanged(newSchool.getPingRange(), newSchool);
      return newSchool;
   }

   public @Nullable Pair<School, SchoolBoundaryIntersectionInfo> findClosestVisibleWritableSchool(EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings, @Nullable School closeCandidate) {
      PingRange visiblePingRange = getVisiblePingRange();
      double x = pingSettings.pingIndexToX(point.pingIndex());
      double closestDistSq = Double.POSITIVE_INFINITY;
      Pair<School, SchoolBoundaryIntersectionInfo> closestSchool = null;
      if (closeCandidate != null && closeCandidate.isWritable() && closeCandidate.intersectsPingRange(visiblePingRange)) {
         SchoolBoundaryIntersectionInfo intersectionInfo = closeCandidate.distanceFrom(point, pingSettings, zSettings, closestDistSq);
         if (intersectionInfo != null) {
            closestDistSq = intersectionInfo.distanceSquared();
            closestSchool = new Pair<>(closeCandidate, intersectionInfo);
         }
      }
      for (School school : schools) {
         PingRange schoolPingRange = school.getPingRange();
         if (schoolPingRange.end().getPingNumber() < point.pingIndex().getPingNumber()) {
            // Entire school is to the left.
            if (closestDistSq <= Utils.sq(x - pingSettings.pingIndexToX(schoolPingRange.end()))) {
               continue;
            }
         } else if (schoolPingRange.begin().getPingNumber() > point.pingIndex().getPingNumber()) {
            // Entire school is to the right.
            if (closestDistSq <= Utils.sq(x - pingSettings.pingIndexToX(schoolPingRange.begin()))) {
               continue;
            }
         }
         if (school.isReadOnly()) {
            continue;
         }
         SchoolBoundaryIntersectionInfo intersectionInfo = school.distanceFrom(point, pingSettings, zSettings, closestDistSq);
         if (intersectionInfo != null) {
            closestDistSq = intersectionInfo.distanceSquared();
            closestSchool = new Pair<>(school, intersectionInfo);
         }
      }
      return closestSchool;
   }

   private SingleSchoolEdit createUndoCopy(School school) {
      return new SchoolMaskEdit(school);
   }

   ChangeManager getUndoChangeManager() {
      return undoChangeManager;
   }

   public void removeAllUndoEdits() {
      undoManager.discardAllEdits();
      undoChangeManager.notifyListeners();
   }

   void addEdit(SchoolEdit schoolEdit) {
      undoManager.addEdit(schoolEdit);
      undoChangeManager.notifyListeners();
   }

   private void addCompositeEdit(List<SingleSchoolEdit> schoolEdits) {
      SchoolEdit edit = schoolEdits.size() == 1 ? schoolEdits.getFirst() : new CompositeSchoolEdit(schoolEdits);
      addEdit(edit);
   }

   boolean canUndo() {
      return undoManager.canUndo();
   }

   PingRange undo() {
      SchoolEdit edit = undoManager.getEditToBeUndone();
      PingRange beforePingRange = edit.getPingRange();
      undoManager.undo();
      for (School school : edit.getSchools()) {
         if (school.isEmpty()) {
            getRegionManager().deleteSchoolWithoutUndo(school);
         }
      }
      undoChangeManager.notifyListeners();
      if (edit.getPingRange().isEmpty()) {
         return beforePingRange;
      } else {
         PingRange modifiedRange = beforePingRange.union(edit.getPingRange());
         getRegionManager().notifyRegionBoundaryChanged(modifiedRange, edit.getSchools());
         return modifiedRange;
      }
   }

   boolean canRedo() {
      return undoManager.canRedo();
   }

   PingRange redo() {
      SchoolEdit edit = undoManager.getEditToBeRedone();
      PingRange beforePingRange = edit.getPingRange();
      undoManager.redo();
      for (School school : edit.getSchools()) {
         if (school.isEmpty()) {
            getRegionManager().deleteSchoolWithoutUndo(school);
         }
      }
      undoChangeManager.notifyListeners();
      if (edit.getPingRange().isEmpty()) {
         return beforePingRange;
      } else {
         PingRange modifiedRange = beforePingRange.union(edit.getPingRange());
         getRegionManager().notifyRegionBoundaryChanged(modifiedRange, edit.getSchools());
         return modifiedRange;
      }
   }

   static Element toXml(PingRange pingRange, Collection<School> schoolsToSave) {
      Element interpretation = DocumentHelper.createElement(XML_SCHOOL_INTERPRETATION);
      schoolsToSave.stream()
            .filter(school -> school.intersectsPingRange(pingRange))
            .sorted(Comparator.comparingInt(School::getObjectNumber))
            .forEach(school -> {
               interpretation.add(school.toXml(pingRange));
            });
      return interpretation;
   }

   void fromXml(Element parentElement, PingRange pingRange) {
      schools.clear();

      Element schoolsElement = parentElement.element(XML_SCHOOL_INTERPRETATION);
      if (schoolsElement == null) {
         return;
      }
      for (Element schoolElement : schoolsElement.elements()) {
         if (schoolElement.getName().equals(School.XML_SCHOOL_MASK_REP)) {
            schools.add(School.createFromXml(getRegionManager(), pingRange.begin(), schoolElement));
         } else if (schoolElement.getName().equals(SchoolOldRep.XML_SCHOOL_REP)) {
            NavigableMap<PingIndex, FloatRangeSet> schoolMask = SchoolOldRep.createSchoolMaskFromXml(getPingContainer(), pingRange.begin(), schoolElement);
            School school = School.create(getRegionManager(), schoolMask);
            school.baseFromXml(schoolElement);
            schools.add(school);
         }
      }
   }

   void join(SchoolManager left, SchoolManager right) {
      left.schools.forEach(school -> school.setRegionManager(getRegionManager()));
      right.schools.forEach(school -> school.setRegionManager(getRegionManager()));
      schools.addAll(left.schools);
      schools.addAll(right.schools);
      mergeSchoolsWithSameObjectNumber();
      removeAllUndoEdits();
   }

   private void mergeSchoolsWithSameObjectNumber() {
      Map<Integer, School> objectMap = new HashMap<>();
      for (School school : schools) {
         int objectNumber = school.getObjectNumber();
         School existingSchool = objectMap.put(objectNumber, school);
         if (existingSchool != null) {
            School mergedSchool = mergeSchools(List.of(existingSchool, school));
            assert mergedSchool != null;
            if (existingSchool.getParameters().equals(school.getParameters())) {
               mergedSchool.setParameters(existingSchool.getParameters());
            }
            // Replace school in map.
            objectMap.put(objectNumber, mergedSchool);
         }
      }
   }

   private abstract static class SchoolEdit extends AbstractUndoableEdit {
      private SchoolEdit() {
      }

      abstract PingRange getPingRange();

      abstract Collection<School> getSchools();
   }

   private abstract class SingleSchoolEdit extends SchoolEdit {
      final School school;
      private final PingRange editPingRange;

      private SingleSchoolEdit(School school) {
         this.school = school;
         editPingRange = school.getPingRange();
      }

      @Override
      PingRange getPingRange() {
         return school.getPingRange();
      }

      @Override
      Collection<School> getSchools() {
         return List.of(school);
      }

      @Override
      public boolean canUndo() {
         PingRange visibleRange = getVisiblePingRange();
         return super.canUndo() && visibleRange.contains(school.getPingRange()) && visibleRange.contains(editPingRange);
      }

      @Override
      public boolean canRedo() {
         PingRange visibleRange = getVisiblePingRange();
         return super.canRedo() && visibleRange.contains(school.getPingRange()) && visibleRange.contains(editPingRange);
      }
   }

   private final class SchoolMaskEdit extends SingleSchoolEdit {
      private NavigableMap<PingIndex, FloatRangeSet> mask;

      private SchoolMaskEdit(School school) {
         super(school);

         //take a copy of current mask
         mask = new TreeMap<>(school.getSchoolMaskRepresentation());
      }

      @Override
      public void undo() {
         super.undo();
         undoRedoAction();
      }

      @Override
      public void redo() {
         super.redo();
         undoRedoAction();
      }

      private void undoRedoAction() {
         NavigableMap<PingIndex, FloatRangeSet> currentMask = new TreeMap<>(school.getSchoolMaskRepresentation());
         school.setMask(mask);
         mask = currentMask;
      }
   }

   private final class SchoolAdd extends SingleSchoolEdit {
      private SchoolAdd(School school) {
         super(school);
      }

      @Override
      public void undo() {
         super.undo();
         getRegionManager().redoDeleteSchool(school);
      }

      @Override
      public void redo() {
         super.redo();
         getRegionManager().redoAddSchool(school);
         school.constrainAfterUndoRedo();
      }
   }

   private final class SchoolRemove extends SingleSchoolEdit {
      private SchoolRemove(School school) {
         super(school);
      }

      @Override
      public void undo() {
         super.undo();
         getRegionManager().redoAddSchool(school);
         school.constrainAfterUndoRedo();
      }

      @Override
      public void redo() {
         super.redo();
         getRegionManager().redoDeleteSchool(school);
      }
   }

   private static final class CompositeSchoolEdit extends SchoolEdit {
      private final List<SingleSchoolEdit> schoolEdits;

      private CompositeSchoolEdit(List<SingleSchoolEdit> schoolEdits) {
         this.schoolEdits = List.copyOf(schoolEdits);
      }

      @Override
      PingRange getPingRange() {
         PingRange result = PingRange.EMPTY_RANGE;
         for (SingleSchoolEdit schoolEdit : schoolEdits) {
            result = result.union(schoolEdit.getPingRange());
         }
         return result;
      }

      @Override
      Collection<School> getSchools() {
         Set<School> schoolSet = new HashSet<>();
         for (SingleSchoolEdit schoolEdit : schoolEdits) {
            schoolSet.addAll(schoolEdit.getSchools());
         }
         return schoolSet;
      }

      @Override
      public void undo() {
         super.undo();
         for (int i = schoolEdits.size() - 1; i >= 0; i--) {
            schoolEdits.get(i).undo();
         }
      }

      @Override
      public void redo() {
         super.redo();
         for (SingleSchoolEdit schoolEdit : schoolEdits) {
            schoolEdit.redo();
         }
      }

      @Override
      public boolean canUndo() {
         boolean result = true;
         for (SingleSchoolEdit schoolEdit : schoolEdits) {
            result &= schoolEdit.canUndo();
         }
         return result;
      }

      @Override
      public boolean canRedo() {
         boolean result = true;
         for (SingleSchoolEdit schoolEdit : schoolEdits) {
            result &= schoolEdit.canRedo();
         }
         return result;
      }
   }

   private static final class SchoolUndoManager extends UndoManager {
      private SchoolUndoManager() {
      }

      private SchoolEdit getEditToBeUndone() {
         return (SchoolEdit) editToBeUndone();
      }

      private SchoolEdit getEditToBeRedone() {
         return (SchoolEdit) editToBeRedone();
      }
   }
}
