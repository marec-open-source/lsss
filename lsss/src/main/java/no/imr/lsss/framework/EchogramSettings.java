package no.imr.lsss.framework;

import no.imr.lsss.modules.echogram.overlays.MaskingEditOverlay;
import no.imr.lsss.modules.echogram.overlays.RegionAddOverlay;
import no.imr.lsss.modules.echogram.overlays.RegionEditOverlay;
import no.imr.lsss.modules.echogram.overlays.ZoomOverlay;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

public final class EchogramSettings {
   public final ObjectParameter<EchogramWorkingMode> workingMode = new ObjectParameter<>(
         new Name("WorkingMode", "Working mode"),
         EchogramWorkingMode.EDIT, EchogramWorkingMode.values(),
         "Change by typing Tab or Shift + Tab");

   private boolean sticky;

   // Edit:

   public final ObjectParameter<RegionEditOverlay.DefaultMode> editSubModeDefault = new ObjectParameter<>(
         new Name("EditSubModeDefault", "Edit sub-mode default"),
         RegionEditOverlay.DefaultMode.SELECT, RegionEditOverlay.DefaultMode.values(),
         "Change by typing Space or Shift + Space");

   public final ObjectParameter<RegionEditOverlay.HorizontalLayerBoundaryMode> editSubModeHorizontalLayerBoundary = new ObjectParameter<>(
         new Name("EditSubModeHorizontalLayerBoundary", "Edit sub-mode horizontal layer boundary"),
         RegionEditOverlay.HorizontalLayerBoundaryMode.DRAW, RegionEditOverlay.HorizontalLayerBoundaryMode.values(),
         "Change by typing Space or Shift + Space");

   public final ObjectParameter<RegionEditOverlay.VerticalLayerBoundaryMode> editSubModeVerticalLayerBoundary = new ObjectParameter<>(
         new Name("EditSubModeVerticalLayerBoundary", "Edit sub-mode vertical layer boundary"),
         RegionEditOverlay.VerticalLayerBoundaryMode.MOVE, RegionEditOverlay.VerticalLayerBoundaryMode.values(),
         "Change by typing Space or Shift + Space");

   public final ObjectParameter<RegionEditOverlay.LayerConnectorMode> editSubModeLayerConnector = new ObjectParameter<>(
         new Name("EditSubModeLayerConnector", "Edit sub-mode layer connector"),
         RegionEditOverlay.LayerConnectorMode.MOVE, RegionEditOverlay.LayerConnectorMode.values(),
         "Change by typing Space or Shift + Space");

   public final ObjectParameter<RegionEditOverlay.SchoolBoundaryMode> editSubModeSchoolBoundary = new ObjectParameter<>(
         new Name("EditSubModeSchoolBoundary", "Edit sub-mode school boundary"),
         RegionEditOverlay.SchoolBoundaryMode.SELECT, RegionEditOverlay.SchoolBoundaryMode.values(),
         "Change by typing Space or Shift + Space");

   public final ObjectParameter<RegionEditOverlay.SchoolInteriorMode> editSubModeSchoolInterior = new ObjectParameter<>(
         new Name("EditSubModeSchoolInterior", "Edit sub-mode school interior"),
         RegionEditOverlay.SchoolInteriorMode.SELECT, RegionEditOverlay.SchoolInteriorMode.values(),
         "Change by typing Space or Shift + Space");

   // Delete:

   public final ObjectParameter<MaskingEditOverlay.MaskingMode> deleteSubMode = new ObjectParameter<>(
         new Name("DeleteSubMode", "Delete sub-mode"),
         MaskingEditOverlay.MaskingMode.UNDELETE_CURRENT, MaskingEditOverlay.MaskingMode.values(),
         "Change by typing Space or Shift + Space");

   public final ObjectParameter<MaskingEditOverlay.DrawMode> deleteDrawMode = new ObjectParameter<>(
         new Name("DeleteDrawMode", "Delete draw mode"),
         MaskingEditOverlay.DrawMode.BOX, MaskingEditOverlay.DrawMode.values(),
         "Change by typing Ctrl + Space");

   public final IntParameter deleteBoxSize = new IntParameter(
         new Name("DeleteBoxSize", "Delete box size"),
         16, Unit.NONE, ValueConstraints.gte(1),
         "Change by typing Plus or Minus");

   public final IntParameter deleteVerticalBarWidth = new IntParameter(
         new Name("DeleteVerticalBarWidth", "Delete vertical bar width"),
         16, Unit.NONE, ValueConstraints.gte(1),
         "Change by typing Plus or Minus");

   public final IntParameter deleteHorizontalBarHeight = new IntParameter(
         new Name("DeleteHorizontalBarHeight", "Delete horizontal bar height"),
         16, Unit.NONE, ValueConstraints.gte(1),
         "Change by typing Plus or Minus");

   // Zoom:

   public final ObjectParameter<ZoomOverlay.Mode> zoomSubMode = new ObjectParameter<>(
         new Name("ZoomSubMode", "Zoom sub-mode"),
         ZoomOverlay.Mode.ZOOM, ZoomOverlay.Mode.values(),
         "Change by typing Space or Shift + Space");

   // Add:

   public final ObjectParameter<RegionAddOverlay.AddMode> addSubMode = new ObjectParameter<>(
         new Name("AddSubMode", "Add sub-mode"),
         RegionAddOverlay.AddMode.HORIZONTAL_BOUNDARY, RegionAddOverlay.AddMode.values(),
         "Change by typing Space or Shift + Space");

   EchogramSettings() {
      workingMode.setPersistable(false);
      workingMode.subscribe(__ -> sticky = false);

      editSubModeDefault.setPersistable(false);
      editSubModeHorizontalLayerBoundary.setPersistable(false);
      editSubModeVerticalLayerBoundary.setPersistable(false);
      editSubModeLayerConnector.setPersistable(false);
      editSubModeSchoolBoundary.setPersistable(false);
      editSubModeSchoolInterior.setPersistable(false);

      deleteSubMode.setPersistable(false);
      deleteDrawMode.setPersistable(false);
      deleteBoxSize.setPersistable(false);
      deleteVerticalBarWidth.setPersistable(false);
      deleteHorizontalBarHeight.setPersistable(false);

      zoomSubMode.setPersistable(false);

      addSubMode.setPersistable(false);
   }

   public void useDefault() {
      workingMode.setValue(EchogramWorkingMode.EDIT);
   }

   public void useDefaultIfNotSticky() {
      if (!sticky) {
         useDefault();
      }
   }

   public void setWorkingModeSticky() {
      sticky = true;
   }
}
