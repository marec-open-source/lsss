package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.region.CurveBoundary;
import no.imr.korona.region.EchogramSelection;
import no.imr.korona.region.Layer;
import no.imr.korona.region.LayerConnector;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.korona.region.SchoolBoundaryIntersectionInfo;
import no.imr.korona.region.VerticalBoundary;
import no.imr.korona.region.schooledit.BoxBoundaryMoveEditor;
import no.imr.korona.region.schooledit.SchoolEditor;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.EchogramSettings;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.EchogramModuleUtils;
import no.imr.lsss.resources.LsssCursors;
import no.imr.tools.Pair;
import no.imr.tools.Utils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.misc.SelectionAction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiListeners;
import org.jspecify.annotations.Nullable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Background overlay for editing regions.
 */
public final class RegionEditOverlay extends BaseEchogramOverlay {
   public enum DefaultMode {
      SELECT, DRAW
   }

   public enum HorizontalLayerBoundaryMode {
      SELECT, DRAW, MOVE
   }

   public enum VerticalLayerBoundaryMode {
      SELECT, MOVE
   }

   public enum LayerConnectorMode {
      SELECT, MOVE
   }

   public enum SchoolBoundaryMode {
      SELECT, DRAW, MOVE, SPLIT, MOVE_BOX_BOUNDARY
   }

   public enum SchoolInteriorMode {
      SELECT, MOVE, SCALE, DRAW
   }

   public static final BasicStroke SELECT_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 5}, 0);

   private final EchogramSettings echogramSettings;

   private @Nullable RegionDisplayOverlay regionDisplayOverlay;
   private RegionInteraction interaction = new DefaultInteraction();
   private @Nullable DragEdit dragEdit;
   private @Nullable School previousClosestSchool;

   public RegionEditOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      echogramSettings = getInterpretationSettings().getEchogramSettings();
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      regionDisplayOverlay = Utils.getFirstOrNull(getEchogramModule().getOverlays(), RegionDisplayOverlay.class);

      registry.add(getInterpretationSettings().getDataFileChangeManager(), __ -> previousClosestSchool = null);
      registry.add(GuiListeners.coalescingLater(this::updateInteractionAndCursor), List.of(
            getInterpretationSettings().getDataFileChangeManager(),
            getEchogramModule().echogramArea()
      ));

      registry.add(GuiListeners.coalescingLater(this::updateCursor), List.of(
            echogramSettings.editSubModeDefault,
            echogramSettings.editSubModeHorizontalLayerBoundary,
            echogramSettings.editSubModeVerticalLayerBoundary,
            echogramSettings.editSubModeLayerConnector,
            echogramSettings.editSubModeSchoolBoundary,
            echogramSettings.editSubModeSchoolInterior
      ));
   }

   @Override
   protected void onDisable() {
      dragEdit = null;
      previousClosestSchool = null;
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return null;
   }

   @Override
   public boolean isBackgroundOverlay() {
      return true;
   }

   private void updateInteractionAndCursor() {
      setDisplayData(null);
      interaction = createInteraction(getEchogramModule().getMousePosition());
      updateCursor();
   }

   private RegionInteraction createInteraction(@Nullable Point point) {
      if (point == null) {
         return new DefaultInteraction();
      }
      EchogramPoint echogramPoint = getEchogramModule().imagePointToEchogramPoint(point);
      if (echogramPoint == null) {
         return new DefaultInteraction();
      }
      Rectangle2D rectangle = BaseOverlaidModule.createRectangle(point);

      Pair<School, SchoolBoundaryIntersectionInfo> closestSchool = findClosestVisibleWritableSchool(echogramPoint);
      if (closestSchool != null) {
         School school = closestSchool.first();

         EchogramPoint centerPoint = school.getCenterPoint();
         if (centerPoint != null && rectangle.contains(getEchogramModule().echogramPointToImagePoint(centerPoint))) {
            return new SchoolInteriorInteraction(school);
         }

         SchoolBoundaryIntersectionInfo intersectionInfo = closestSchool.second();
         EchogramPoint startPoint = intersectionInfo.getStartPoint();
         EchogramPoint endPoint = intersectionInfo.getEndPoint();
         Line2D.Double line = new Line2D.Double(getEchogramModule().echogramPointToImagePoint(startPoint), getEchogramModule().echogramPointToImagePoint(endPoint));
         if (rectangle.intersectsLine(line)) {
            BoxBoundaryMoveEditor.BoxEditMode boxEditMode = school.isBoxMode(getZSettings().getDepthTransform())
                  ? BoxBoundaryMoveEditor.pointToMode(echogramPoint, school.getPingRange(), BoxBoundaryMoveEditor.zRange(school, getZSettings()), getPingSettings(), getZSettings())
                  : BoxBoundaryMoveEditor.BoxEditMode.LEFT;
            return new SchoolBoundaryInteraction(school, boxEditMode);
         }

         if (school.contains(echogramPoint)) {
            return new SchoolInteriorInteraction(school);
         }
      }

      LayerConnector layerConnector = regionDisplayOverlay == null || regionDisplayOverlay.showConnectors.getBooleanValue()
            ? getRegionManager().getLayerManager().findClosestLayerConnector(echogramPoint)
            : null;
      if (layerConnector != null) {
         Point2D p = getEchogramModule().echogramPointToImagePoint(layerConnector.getPoint());
         if (rectangle.contains(p) && !getRegionManager().isReadOnlyIncludingEnd(layerConnector.getPingIndex())) {
            return new LayerConnectorInteraction(layerConnector);
         }
      }

      VerticalBoundary verticalBoundary = getRegionManager().getLayerManager().findClosestVerticalBoundary(echogramPoint,
            getZSettings().getZoomedZRange(), getZSettings().getDepthTransform());
      if (verticalBoundary != null) {
         float x = getPingSettings().pingIndexToX(verticalBoundary.getPingIndex());
         if (rectangle.contains(x, point.getY()) && !getRegionManager().isReadOnlyIncludingEnd(verticalBoundary.getPingIndex())) {
            return new VerticalBoundaryInteraction(verticalBoundary);
         }
      }

      CurveBoundary curveBoundary = findClosestWritableCurveBoundary(echogramPoint);
      if (curveBoundary != null) {
         float depth = curveBoundary.getCurve().getClampedDepth(echogramPoint.pingIndex());
         float y = getZSettings().depthToY(depth, echogramPoint.pingIndex());
         if (rectangle.contains(point.getX(), y)) {
            return new CurveBoundaryInteraction(curveBoundary);
         }
      }

      return new DefaultInteraction();
   }

   private @Nullable CurveBoundary findClosestWritableCurveBoundary(EchogramPoint echogramPoint) {
      FloatRange depthRange = getZSettings().getDepthTransform().zToDepth(getZSettings().getZoomedZRange(), echogramPoint.pingIndex());
      CurveBoundary curveBoundary = getRegionManager().getLayerManager().findClosestCurveBoundary(echogramPoint, depthRange);
      return curveBoundary == null || getRegionManager().isReadOnly(curveBoundary.getPingRange()) ? null : curveBoundary;
   }

   private @Nullable Pair<School, SchoolBoundaryIntersectionInfo> findClosestVisibleWritableSchool(EchogramPoint echogramPoint) {
      Pair<School, SchoolBoundaryIntersectionInfo> schoolAndBorder = getRegionManager().getSchoolManager().findClosestVisibleWritableSchool(
            echogramPoint, getPingSettings(), getZSettings(), previousClosestSchool);
      previousClosestSchool = schoolAndBorder != null ? schoolAndBorder.first() : null;
      return schoolAndBorder;
   }

   @Override
   public void onActivate() {
      updateInteractionAndCursor();
   }

   @Override
   public void onDeactivate() {
      if (getEchogramModule().isActiveOverlayLocked()) {
         release();
         getEchogramModule().setActiveOverlayLocked(false);
      }
   }

   private void updateCursor() {
      setCursor(interaction.getCursor());
   }

   @Override
   public boolean keyTyped(KeyEvent keyEvent) {
      switch (keyEvent.getKeyChar()) {
         case ' ' -> {
            if (dragEdit != null) {
               dragEdit.shiftDragMode();
            } else {
               interaction.shiftSubMode(keyEvent.isShiftDown() ? -1 : 1);
            }
         }
         case KeyEvent.VK_ESCAPE -> {
            if (dragEdit != null) {
               dragEdit.cancelEdit();
               dragEdit = null;
            }
            updateInteractionAndCursor();
            getEchogramModule().setActiveOverlayLocked(false);
            getEchogramModule().updateActiveOverlay();
         }
         default -> {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_1, KeyEvent.VK_NUMPAD1 -> {
            if (getEchogramModule().isAnyMouseButtonPressed()) {
               break;
            }
            getEchogramModule().setActiveOverlayLocked(!getEchogramModule().isActiveOverlayLocked());
            if (getEchogramModule().isActiveOverlayLocked()) {
               grab();
            } else {
               release();
            }
         }
         case KeyEvent.VK_2, KeyEvent.VK_NUMPAD2 -> {
            setDrawGlobal();
         }
         case KeyEvent.VK_3, KeyEvent.VK_NUMPAD3 -> {
            setSelectionGlobal();
         }
         case KeyEvent.VK_4, KeyEvent.VK_NUMPAD4 -> {
            setMoveGlobal();
         }
         default -> {
            return false;
         }
      }
      return true;
   }

   private void setMoveGlobal() {
      boolean dragging = dragEdit != null;
      if (dragging) {
         release();
      }
      echogramSettings.editSubModeDefault.setValue(DefaultMode.DRAW); // No MOVE in DefaultMode
      echogramSettings.editSubModeHorizontalLayerBoundary.setValue(HorizontalLayerBoundaryMode.MOVE);
      echogramSettings.editSubModeVerticalLayerBoundary.setValue(VerticalLayerBoundaryMode.MOVE);
      echogramSettings.editSubModeLayerConnector.setValue(LayerConnectorMode.MOVE);
      echogramSettings.editSubModeSchoolBoundary.setValue(SchoolBoundaryMode.MOVE);
      echogramSettings.editSubModeSchoolInterior.setValue(SchoolInteriorMode.MOVE);
      if (dragging) {
         grab();
      }
   }

   private void setSelectionGlobal() {
      boolean dragging = dragEdit != null;
      if (dragging) {
         release();
      }
      echogramSettings.editSubModeDefault.setValue(DefaultMode.SELECT);
      echogramSettings.editSubModeHorizontalLayerBoundary.setValue(HorizontalLayerBoundaryMode.SELECT);
      echogramSettings.editSubModeVerticalLayerBoundary.setValue(VerticalLayerBoundaryMode.SELECT);
      echogramSettings.editSubModeLayerConnector.setValue(LayerConnectorMode.SELECT);
      echogramSettings.editSubModeSchoolBoundary.setValue(SchoolBoundaryMode.SELECT);
      echogramSettings.editSubModeSchoolInterior.setValue(SchoolInteriorMode.SELECT);
      if (dragging) {
         grab();
      }
   }

   private void setDrawGlobal() {
      boolean dragging = dragEdit != null;
      if (dragging) {
         release();
      }
      echogramSettings.editSubModeDefault.setValue(DefaultMode.DRAW);
      echogramSettings.editSubModeHorizontalLayerBoundary.setValue(HorizontalLayerBoundaryMode.DRAW);
      echogramSettings.editSubModeVerticalLayerBoundary.setValue(VerticalLayerBoundaryMode.MOVE); // No DRAW in VerticalBoundaryMode
      echogramSettings.editSubModeLayerConnector.setValue(LayerConnectorMode.MOVE);     // No DRAW in LayerConnectorMode
      echogramSettings.editSubModeSchoolBoundary.setValue(SchoolBoundaryMode.DRAW);
      echogramSettings.editSubModeSchoolInterior.setValue(SchoolInteriorMode.DRAW);
      if (dragging) {
         grab();
      }
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      if (getEchogramModule().isActiveOverlayLocked()) {
         getEchogramModule().setActiveOverlayLocked(false);
      } else {
         grab();
      }
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      if (getEchogramModule().isActiveOverlayLocked()) {
         getEchogramModule().setActiveOverlayLocked(false);
      } else {
         release();
      }
   }

   @Override
   public void mouseExited(MouseEvent mouseEvent) {
      //if mouse exits, perform a last drag event and release the state
      if (dragEdit != null) {
         mouseDragged(mouseEvent);
         mouseReleased(mouseEvent);
      }
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      drag(mouseEvent.getPoint());
   }

   @Override
   public void mouseMoved(MouseEvent mouseEvent) {
      if (getEchogramModule().isActiveOverlayLocked()) {
         drag(mouseEvent.getPoint());
      } else {
         updateInteractionAndCursor();
      }
   }

   private void grab() {
      if (dragEdit != null) {
         return;
      }
      Point mousePosition = getEchogramModule().getMousePosition();
      if (mousePosition == null) {
         return;
      }
      EchogramPoint echogramPoint = imagePointToClampedEchogramPoint(mousePosition);
      dragEdit = interaction.grab(echogramPoint);
      drag(mousePosition);
   }

   private EchogramPoint imagePointToClampedEchogramPoint(Point2D point) {
      return EchogramModuleUtils.imagePointToClampedEchogramPoint(point, getPingSettings(), getZSettings());
   }

   private void release() {
      if (dragEdit == null) {
         return;
      }
      dragEdit.endEditing();
      dragEdit = null;
      updateInteractionAndCursor();
      getEchogramModule().updateActiveOverlay();
   }

   private void drag(Point point) {
      if (dragEdit == null) {
         return;
      }
      dragEdit.dragTo(imagePointToClampedEchogramPoint(point));
   }

   public @Nullable CurveBoundary getActiveCurveBoundary() {
      return interaction instanceof CurveBoundaryInteraction curveBoundaryInteraction
            ? curveBoundaryInteraction.curveBoundary
            : null;
   }

   // ----------------------------

   private abstract static class DragEdit {
      final EchogramPoint startDragPoint;
      EchogramPoint endDragPoint;

      private DragEdit(EchogramPoint echogramPoint) {
         startDragPoint = echogramPoint;
         endDragPoint = echogramPoint;
      }

      private void dragTo(EchogramPoint echogramPoint) {
         EchogramPoint from = endDragPoint;
         endDragPoint = echogramPoint;
         drag(from, echogramPoint);
      }

      abstract void drag(EchogramPoint fromPoint, EchogramPoint toPoint);

      void shiftDragMode() {
      }

      void endEditing() {
      }

      void cancelEdit() {
      }
   }

   private final class Select extends DragEdit {
      private final @Nullable Region selectOnClickRegion;
      private boolean hasDragged;

      private Select(EchogramPoint echogramPoint, @Nullable Region selectOnClickRegion) {
         super(echogramPoint);

         this.selectOnClickRegion = selectOnClickRegion;
      }

      private Select(EchogramPoint echogramPoint) {
         this(echogramPoint, null);
      }

      @Override
      void drag(EchogramPoint fromPoint, EchogramPoint toPoint) {
         hasDragged |= !startDragPoint.equals(toPoint);
         EchogramRectangle selectionRectangle = getSelectionEchogramRectangle();
         getEchogramModule().setSelectionRectangle(selectionRectangle);
         Rectangle2D.Float box = EchogramModuleUtils.toImageRectangle(selectionRectangle, getPingSettings(), getZSettings());
         setDisplayData(new DisplayData(box));
      }

      @Override
      void endEditing() {
         SelectionAction action = SelectionAction.fromModifiersEx(getEchogramModule().getModifiersEx());
         if (hasDragged || selectOnClickRegion == null) {
            EchogramRectangle echogramRectangle = getSelectionEchogramRectangle();
            getRegionManager().doEchogramSelection(new EchogramSelection(action, echogramRectangle));
         } else {
            getRegionManager().doSelection(List.of(selectOnClickRegion), action);
         }
         getEchogramModule().setSelectionRectangle(null);
         setDisplayData(null);
      }

      private EchogramRectangle getSelectionEchogramRectangle() {
         return EchogramModuleUtils.toSelectionEchogramRectangle(startDragPoint, endDragPoint, getPingSettings(), getZSettings());
      }

      @Override
      void cancelEdit() {
         getEchogramModule().setSelectionRectangle(null);
         setDisplayData(null);
      }
   }

   private abstract class BaseLayerDragEdit extends DragEdit {
      private final Supplier<Collection<Layer>> layers;

      private BaseLayerDragEdit(EchogramPoint echogramPoint, Supplier<Collection<Layer>> layers) {
         super(echogramPoint);
         this.layers = layers;
         getInterpretationSettings().getBottomZSettings().freezeBottomBoundaryDepthTransform();
      }

      @Override
      void endEditing() {
         getRegionManager().getLayerManager().verifyAndAdjustConnectors(layers.get());
         getInterpretationSettings().getBottomZSettings().resetBottomBoundaryDepthTransform();
      }

      @Override
      void cancelEdit() {
         endEditing();
      }
   }

   private final class CurveBoundaryDraw extends BaseLayerDragEdit {
      private final CurveBoundary curveBoundary;

      private CurveBoundaryDraw(EchogramPoint echogramPoint, CurveBoundary curveBoundary) {
         super(echogramPoint, curveBoundary::getLayers);
         this.curveBoundary = curveBoundary;
      }

      @Override
      void drag(EchogramPoint fromPoint, EchogramPoint toPoint) {
         getRegionManager().getLayerManager().editBoundary(fromPoint, toPoint, getZSettings().getDepthTransform(), curveBoundary);
      }
   }

   private final class CurveBoundaryMove extends BaseLayerDragEdit {
      private final CurveBoundary curveBoundary;

      private CurveBoundaryMove(EchogramPoint echogramPoint, CurveBoundary curveBoundary) {
         super(echogramPoint, curveBoundary::getLayers);
         this.curveBoundary = curveBoundary;
      }

      @Override
      void drag(EchogramPoint fromPoint, EchogramPoint toPoint) {
         getRegionManager().getLayerManager().moveBoundary(toPoint, curveBoundary);
      }
   }

   private final class VerticalBoundaryMove extends BaseLayerDragEdit {
      private final VerticalBoundary verticalBoundary;

      private VerticalBoundaryMove(EchogramPoint echogramPoint, VerticalBoundary verticalBoundary) {
         super(echogramPoint, verticalBoundary::getLayers);
         this.verticalBoundary = verticalBoundary;
      }

      @Override
      void drag(EchogramPoint fromPoint, EchogramPoint toPoint) {
         getRegionManager().getLayerManager().editVerticalBoundary(toPoint, getZSettings().getDepthTransform(), verticalBoundary);
      }
   }

   private final class LayerConnectorMove extends BaseLayerDragEdit {
      private final LayerConnector layerConnector;

      private LayerConnectorMove(EchogramPoint echogramPoint, LayerConnector layerConnector) {
         super(echogramPoint, layerConnector::getLayers);
         this.layerConnector = layerConnector;
      }

      @Override
      void drag(EchogramPoint fromPoint, EchogramPoint toPoint) {
         getRegionManager().getLayerManager().editConnector(toPoint, getZSettings().getDepthTransform(), layerConnector);
      }
   }

   private abstract class BaseSchoolDragEdit extends DragEdit {
      private final SchoolEditor editor;

      private BaseSchoolDragEdit(EchogramPoint echogramPoint, SchoolEditor editor) {
         super(echogramPoint);

         this.editor = editor;
      }

      @Override
      void drag(EchogramPoint fromPoint, EchogramPoint toPoint) {
         editor.edit(fromPoint, toPoint);
      }

      @Override
      void shiftDragMode() {
         editor.switchPointsToKeep();
      }

      @Override
      void endEditing() {
         editor.confirm();
      }

      @Override
      void cancelEdit() {
         getRegionManager().getSchoolManager().cancelEdit();
      }
   }

   private final class SchoolBoundarySplit extends BaseSchoolDragEdit {
      private SchoolBoundarySplit(EchogramPoint echogramPoint, School school) {
         super(echogramPoint, school.boundarySplitStart(echogramPoint, getPingSettings(), getZSettings()));
      }
   }

   private final class SchoolBoxBoundaryMove extends BaseSchoolDragEdit {
      private SchoolBoxBoundaryMove(EchogramPoint echogramPoint, School school) {
         super(echogramPoint, school.boxBoundaryMoveStart(echogramPoint, getPingSettings(), getZSettings()));
      }
   }

   private final class SchoolMove extends BaseSchoolDragEdit {
      private SchoolMove(EchogramPoint echogramPoint, School school) {
         super(echogramPoint, school.moveStart(echogramPoint, getPingSettings(), getZSettings()));
      }
   }

   private final class SchoolBoundaryDraw extends BaseSchoolDragEdit {
      private SchoolBoundaryDraw(EchogramPoint echogramPoint, School school) {
         super(echogramPoint, school.boundaryDrawStart(echogramPoint, getPingSettings(), getZSettings()));
      }
   }

   private final class SchoolScale extends BaseSchoolDragEdit {
      private SchoolScale(EchogramPoint echogramPoint, School school) {
         super(echogramPoint, school.scaleStart(echogramPoint, getPingSettings(), getZSettings()));
      }
   }

   // ----------------------------

   private abstract static class RegionInteraction {
      private RegionInteraction() {
      }

      abstract void shiftSubMode(int shift);

      abstract Cursor getCursor();

      abstract DragEdit grab(EchogramPoint echogramPoint);
   }

   private final class DefaultInteraction extends RegionInteraction {
      private DefaultInteraction() {
      }

      @Override
      void shiftSubMode(int shift) {
         echogramSettings.editSubModeDefault.shiftValue(shift);
         switch (echogramSettings.editSubModeDefault.getValue()) {
            case SELECT -> setSelectionGlobal();
            case DRAW -> setDrawGlobal();
         }
      }

      @Override
      Cursor getCursor() {
         return switch (echogramSettings.editSubModeDefault.getValue()) {
            case SELECT -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            case DRAW -> LsssCursors.EDIT;
         };
      }

      @Override
      DragEdit grab(EchogramPoint echogramPoint) {
         return switch (echogramSettings.editSubModeDefault.getValue()) {
            case SELECT -> {
               yield new Select(echogramPoint);
            }
            case DRAW -> {
               Pair<School, SchoolBoundaryIntersectionInfo> closestSchool = findClosestVisibleWritableSchool(echogramPoint);
               School school = closestSchool != null ? closestSchool.first() : null;
               double distToSchool = closestSchool != null ? closestSchool.second().distanceSquared() : Double.POSITIVE_INFINITY;

               CurveBoundary curveBoundary = findClosestWritableCurveBoundary(echogramPoint);
               double distToCurveBoundary;
               if (curveBoundary != null) {
                  PingIndex pingIndex = echogramPoint.pingIndex();
                  float y1 = getZSettings().depthToY(curveBoundary.getCurve().getClampedDepth(pingIndex), pingIndex);
                  float y2 = getZSettings().depthToY(echogramPoint.depth(), pingIndex);
                  distToCurveBoundary = Utils.sq(y1 - y2);
               } else {
                  distToCurveBoundary = Double.POSITIVE_INFINITY;
               }

               if (curveBoundary != null && distToCurveBoundary < distToSchool) {
                  yield new CurveBoundaryDraw(echogramPoint, curveBoundary);
               } else if (school != null) {
                  yield new SchoolBoundaryDraw(echogramPoint, school);
               }
               yield new Select(echogramPoint);
            }
         };
      }
   }

   private final class CurveBoundaryInteraction extends RegionInteraction {
      private final CurveBoundary curveBoundary;

      private CurveBoundaryInteraction(CurveBoundary curveBoundary) {
         this.curveBoundary = curveBoundary;
      }

      @Override
      void shiftSubMode(int shift) {
         echogramSettings.editSubModeHorizontalLayerBoundary.shiftValue(shift);
         if (echogramSettings.editSubModeHorizontalLayerBoundary.getValue() == HorizontalLayerBoundaryMode.SELECT) {
            setSelectionGlobal();
         }
      }

      @Override
      Cursor getCursor() {
         return switch (echogramSettings.editSubModeHorizontalLayerBoundary.getValue()) {
            case SELECT -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            case DRAW -> LsssCursors.EDIT;
            case MOVE -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
         };
      }

      @Override
      DragEdit grab(EchogramPoint echogramPoint) {
         return switch (echogramSettings.editSubModeHorizontalLayerBoundary.getValue()) {
            case SELECT -> new Select(echogramPoint);
            case DRAW -> new CurveBoundaryDraw(echogramPoint, curveBoundary);
            case MOVE -> new CurveBoundaryMove(echogramPoint, curveBoundary);
         };
      }
   }

   private final class VerticalBoundaryInteraction extends RegionInteraction {
      private final VerticalBoundary verticalBoundary;

      private VerticalBoundaryInteraction(VerticalBoundary verticalBoundary) {
         this.verticalBoundary = verticalBoundary;
      }

      @Override
      void shiftSubMode(int shift) {
         echogramSettings.editSubModeVerticalLayerBoundary.shiftValue(shift);
         if (echogramSettings.editSubModeVerticalLayerBoundary.getValue() == VerticalLayerBoundaryMode.SELECT) {
            setSelectionGlobal();
         }
      }

      @Override
      Cursor getCursor() {
         return switch (echogramSettings.editSubModeVerticalLayerBoundary.getValue()) {
            case SELECT -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            case MOVE -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
         };
      }

      @Override
      DragEdit grab(EchogramPoint echogramPoint) {
         return switch (echogramSettings.editSubModeVerticalLayerBoundary.getValue()) {
            case SELECT -> new Select(echogramPoint);
            case MOVE -> new VerticalBoundaryMove(echogramPoint, verticalBoundary);
         };
      }
   }

   private final class LayerConnectorInteraction extends RegionInteraction {
      private final LayerConnector layerConnector;

      private LayerConnectorInteraction(LayerConnector layerConnector) {
         this.layerConnector = layerConnector;
      }

      @Override
      void shiftSubMode(int shift) {
         echogramSettings.editSubModeLayerConnector.shiftValue(shift);
         if (echogramSettings.editSubModeLayerConnector.getValue() == LayerConnectorMode.SELECT) {
            setSelectionGlobal();
         }
      }

      @Override
      Cursor getCursor() {
         return switch (echogramSettings.editSubModeLayerConnector.getValue()) {
            case SELECT -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            case MOVE -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
         };
      }

      @Override
      DragEdit grab(EchogramPoint echogramPoint) {
         return switch (echogramSettings.editSubModeLayerConnector.getValue()) {
            case SELECT -> new Select(echogramPoint);
            case MOVE -> new LayerConnectorMove(echogramPoint, layerConnector);
         };
      }
   }

   private final class SchoolBoundaryInteraction extends RegionInteraction {
      private final School school;
      private final BoxBoundaryMoveEditor.BoxEditMode boxEditMode;
      private final boolean boxMode;

      private SchoolBoundaryInteraction(School school, BoxBoundaryMoveEditor.BoxEditMode boxEditMode) {
         this.school = school;
         this.boxEditMode = boxEditMode;
         boxMode = school.isBoxMode(getZSettings().getDepthTransform());
      }

      @Override
      void shiftSubMode(int shift) {
         echogramSettings.editSubModeSchoolBoundary.shiftValue(shift);
         if (echogramSettings.editSubModeSchoolBoundary.getValue() == SchoolBoundaryMode.MOVE_BOX_BOUNDARY && !boxMode) {
            echogramSettings.editSubModeSchoolBoundary.shiftValue(shift);
         }
         if (echogramSettings.editSubModeSchoolBoundary.getValue() == SchoolBoundaryMode.SELECT) {
            setSelectionGlobal();
         }
      }

      @Override
      Cursor getCursor() {
         return switch (echogramSettings.editSubModeSchoolBoundary.getValue()) {
            case SELECT -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            case DRAW -> LsssCursors.EDIT;
            case MOVE -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            case SPLIT -> LsssCursors.SPLIT;
            case MOVE_BOX_BOUNDARY -> boxMode
                  ? boxModeCursor(boxEditMode)
                  : LsssCursors.EDIT;
         };
      }

      private static Cursor boxModeCursor(BoxBoundaryMoveEditor.BoxEditMode boxEditMode) {
         int type = switch (boxEditMode) {
            case LEFT -> Cursor.W_RESIZE_CURSOR;
            case RIGHT -> Cursor.E_RESIZE_CURSOR;
            case TOP -> Cursor.N_RESIZE_CURSOR;
            case BOTTOM -> Cursor.S_RESIZE_CURSOR;
            case TOP_LEFT -> Cursor.NW_RESIZE_CURSOR;
            case TOP_RIGHT -> Cursor.NE_RESIZE_CURSOR;
            case BOTTOM_LEFT -> Cursor.SW_RESIZE_CURSOR;
            case BOTTOM_RIGHT -> Cursor.SE_RESIZE_CURSOR;
         };
         return Cursor.getPredefinedCursor(type);
      }

      @Override
      DragEdit grab(EchogramPoint echogramPoint) {
         return switch (echogramSettings.editSubModeSchoolBoundary.getValue()) {
            case SELECT -> new Select(echogramPoint, school);
            case DRAW -> new SchoolBoundaryDraw(echogramPoint, school);
            case MOVE -> new SchoolMove(echogramPoint, school);
            case SPLIT -> new SchoolBoundarySplit(echogramPoint, school);
            case MOVE_BOX_BOUNDARY -> boxMode
                  ? new SchoolBoxBoundaryMove(echogramPoint, school)
                  : new SchoolBoundaryDraw(echogramPoint, school);
         };
      }
   }

   private final class SchoolInteriorInteraction extends RegionInteraction {
      private final School school;

      private SchoolInteriorInteraction(School school) {
         this.school = school;
      }

      @Override
      void shiftSubMode(int shift) {
         echogramSettings.editSubModeSchoolInterior.shiftValue(shift);
         switch (echogramSettings.editSubModeSchoolInterior.getValue()) {
            case SELECT -> setSelectionGlobal();
            case DRAW -> setDrawGlobal();
            default -> {
            }
         }
      }

      @Override
      Cursor getCursor() {
         return switch (echogramSettings.editSubModeSchoolInterior.getValue()) {
            case SELECT -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            case MOVE -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            case SCALE -> LsssCursors.SCALE;
            case DRAW -> LsssCursors.EDIT;
         };
      }

      @Override
      DragEdit grab(EchogramPoint echogramPoint) {
         return switch (echogramSettings.editSubModeSchoolInterior.getValue()) {
            case SELECT -> new Select(echogramPoint, school);
            case MOVE -> new SchoolMove(echogramPoint, school);
            case SCALE -> new SchoolScale(echogramPoint, school);
            case DRAW -> new SchoolBoundaryInteraction(school, BoxBoundaryMoveEditor.BoxEditMode.LEFT).grab(echogramPoint);
         };
      }
   }

   // ----------------------------

   private static final class DisplayData extends OverlayDisplayData {
      private final Rectangle2D selectionBox;

      private DisplayData(Rectangle2D selectionBox) {
         this.selectionBox = selectionBox;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(SELECT_STROKE);
         g2d.setColor(Color.RED);
         g2d.draw(selectionBox);
      }
   }
}
