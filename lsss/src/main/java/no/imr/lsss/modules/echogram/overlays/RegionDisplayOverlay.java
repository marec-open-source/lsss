package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.Curve;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.CurveBoundary;
import no.imr.korona.region.Layer;
import no.imr.korona.region.LayerConnector;
import no.imr.korona.region.School;
import no.imr.korona.region.SchoolBoundaryObject;
import no.imr.korona.region.VerticalBoundary;
import no.imr.korona.region.schooledit.SchoolEditor;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.linestrip.BoundedLineStripBuilder;
import no.imr.tools.swing.linestrip.CountingLineStripBuilder;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.imr.tools.swing.linestrip.PathLineStripBuilder;
import no.marec.lsss.api.util.LineStripBuilder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws regions.
 */
public final class RegionDisplayOverlay extends BaseEchogramOverlay {
   private static final float CONNECTOR_RADIUS = 2.5f;
   private static final float SCHOOL_CENTER_POINT_RADIUS = 4;

   private final IntParameter selectedLineThickness = new IntParameter(
         new Name("SelectedLineThickness", "Selected line thickness"),
         2, Unit.COUNT, ValueConstraints.gte(0),
         "Thickness of borders of selected regions");

   final BooleanParameter showConnectors = new BooleanParameter(
         new Name("ShowConnectors", "Show connectors"),
         true,
         "Show layer connectors");

   private Stroke selectedStroke = createStroke();

   public RegionDisplayOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      selectedLineThickness.subscribe(_ -> {
         selectedStroke = createStroke();
         repaint();
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            selectedLineThickness,
            showConnectors
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            showConnectors,
            getEchogramModule().echogramArea(),
            getRegionManager().selectedRegions(),
            getRegionManager().getRegionBoundaryChangeManager(),
            getRegionManager().getRegionDeletedChangeManager()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return getInterpretationSettings().getPingRange().isEmpty() ? null : new DisplayDataBuilder().displayData;
   }

   private BasicStroke createStroke() {
      return new BasicStroke(selectedLineThickness.getIntValue());
   }

   private final class DisplayDataBuilder {
      private final EchogramPingSettings pingSettings = getPingSettings();
      private final EchogramZSettings zSettings = getZSettings();
      private final PingRange pingRange = getInterpretationSettings().getPingRange();
      private final Rectangle bounds = getEchogramModule().getBounds();
      private final Set<Object> renderedObjects = new HashSet<>();

      private final DisplayData displayData = new DisplayData();
      private final CountingLineStripBuilder countingSelectedBoundaryPath = new CountingLineStripBuilder(new PathLineStripBuilder(displayData.selectedBoundaryPath));
      private final LineStripBuilder selectedBoundaryPath = LineStripBuilders.piecewiseHorizontal(countingSelectedBoundaryPath, bounds);
      private final LineStripBuilder unselectedBoundaryPath = LineStripBuilders.piecewiseHorizontal(displayData.unselectedBoundaryPath, bounds);
      private final LineStripBuilder connectorPath = LineStripBuilders.piecewiseHorizontal(displayData.connectorPath, bounds);
      private final LineStripBuilder schoolEditPath = LineStripBuilders.piecewiseHorizontal(displayData.schoolEditPath, bounds);
      private final LineStripBuilder schoolRemovePath = LineStripBuilders.piecewiseHorizontal(displayData.schoolRemovePath, bounds);
      private final LineStripBuilder selectedSchoolCenterPointPath = new BoundedLineStripBuilder(new PathLineStripBuilder(displayData.selectedSchoolCenterPointPath), bounds);
      private final LineStripBuilder unselectedSchoolCenterPointPath = new BoundedLineStripBuilder(new PathLineStripBuilder(displayData.unselectedSchoolCenterPointPath), bounds);

      private DisplayDataBuilder() {
         for (Layer layer : getRegionManager().getLayerManager().getSelectedRegions()) {
            renderLayer(layer, selectedBoundaryPath);
         }
         for (Layer layer : getRegionManager().getLayerManager().getLayers()) {
            if (!layer.isSelected()) {
               renderLayer(layer, unselectedBoundaryPath);
            }
         }

         int schoolCount = 0;
         for (School school : getRegionManager().getSchoolManager().getSchools()) {
            if (school.intersectsPingRange(pingRange)) {
               schoolCount++;
               renderSchool(school);
            }
         }
         displayData.longSelectedBoundary = countingSelectedBoundaryPath.getPointCount() > 10_000;
         displayData.manySchools = schoolCount > 2000;
      }

      private void renderLayer(Layer layer, LineStripBuilder pathBuilder) {
         if (!layer.intersectsPingRange(pingRange)) {
            return;
         }
         for (VerticalBoundary boundary : layer.getVerticalBoundaries()) {
            renderVerticalBoundary(boundary, pathBuilder);
         }
         for (CurveBoundary boundary : layer.getUpperCurveBoundaries()) {
            renderCurveBoundary(boundary, pathBuilder);
         }
         for (CurveBoundary boundary : layer.getLowerCurveBoundaries()) {
            renderCurveBoundary(boundary, pathBuilder);
         }
      }

      private void renderConnector(LayerConnector layerConnector) {
         if (!renderedObjects.add(layerConnector)) {
            return;
         }
         float x = pingSettings.pingIndexToX(layerConnector.getPingIndex());
         float y = zSettings.depthToY(layerConnector.getDepth(), layerConnector.getPingIndex());
         connectorPath.addPoint(x - CONNECTOR_RADIUS, y - CONNECTOR_RADIUS);
         connectorPath.addPoint(x + CONNECTOR_RADIUS, y - CONNECTOR_RADIUS);
         connectorPath.addPoint(x + CONNECTOR_RADIUS, y + CONNECTOR_RADIUS);
         connectorPath.addPoint(x - CONNECTOR_RADIUS, y + CONNECTOR_RADIUS);
         connectorPath.addPoint(x - CONNECTOR_RADIUS, y - CONNECTOR_RADIUS);
         connectorPath.endLineStrip();
      }

      private void renderVerticalBoundary(VerticalBoundary verticalBoundary, LineStripBuilder pathBuilder) {
         if (!renderedObjects.add(verticalBoundary)) {
            return;
         }
         if (showConnectors.getBooleanValue()) {
            renderConnector(verticalBoundary.getStartConnector());
            renderConnector(verticalBoundary.getEndConnector());
         }
         PingIndex pingIndex = verticalBoundary.getPingIndex();
         float x = pingSettings.pingIndexToX(pingIndex);
         float y0 = zSettings.depthToY(verticalBoundary.getMinDepth(), pingIndex);
         float y1 = zSettings.depthToY(verticalBoundary.getMaxDepth(), pingIndex);
         pathBuilder.addPoint(x, y0);
         pathBuilder.addPoint(x, y1);
         pathBuilder.endLineStrip();
      }

      private void renderCurveBoundary(CurveBoundary curveBoundary, LineStripBuilder pathBuilder) {
         if (!renderedObjects.add(curveBoundary)) {
            return;
         }
         Curve curve = curveBoundary.getCurve();
         PingRange curveRange = curve.getPingRange();

         addPoint(pathBuilder, curveRange.begin(), curve.getDepth(curveRange.begin()));
         for (PingIndex pingIndex : getInterpretationSettings().getPingSampler().getRequestedPingIndices(curveRange)) {
            addPoint(pathBuilder, pingIndex, curve.getDepth(pingIndex));
         }
         addPoint(pathBuilder, curveRange.end(), curve.getLastDepth());
         addPoint(pathBuilder, curveBoundary.getEndConnector().getPoint());

         pathBuilder.endLineStrip();
      }

      private void renderSchool(School school) {
         renderSchoolCenter(school);
         renderSchoolBoundary(school);
      }

      private void renderSchoolCenter(School school) {
         EchogramPoint centerPoint = school.getCenterPoint();
         if (centerPoint == null) {
            return;
         }
         float x = pingSettings.pingIndexToX(centerPoint.pingIndex());
         float y = zSettings.depthToY(centerPoint.depth(), centerPoint.pingIndex());
         LineStripBuilder pathBuilder = school.isSelected() ? selectedSchoolCenterPointPath : unselectedSchoolCenterPointPath;
         pathBuilder.addPoint(x - SCHOOL_CENTER_POINT_RADIUS, y - SCHOOL_CENTER_POINT_RADIUS);
         pathBuilder.addPoint(x + SCHOOL_CENTER_POINT_RADIUS, y + SCHOOL_CENTER_POINT_RADIUS);
         pathBuilder.endLineStrip();
         pathBuilder.addPoint(x - SCHOOL_CENTER_POINT_RADIUS, y + SCHOOL_CENTER_POINT_RADIUS);
         pathBuilder.addPoint(x + SCHOOL_CENTER_POINT_RADIUS, y - SCHOOL_CENTER_POINT_RADIUS);
         pathBuilder.endLineStrip();
      }

      private void renderSchoolBoundary(School school) {
         LineStripBuilder pathBuilder = school.isSelected() ? selectedBoundaryPath : unselectedBoundaryPath;
         SchoolEditor editor = school.getEditor();
         if (editor != null) {
            // Render points to be kept.
            for (List<EchogramPoint> echogramPoints : editor.getSortedToBeRemovedComplement()) {
               renderPath(pathBuilder, echogramPoints);
            }
            // Render points to be removed.
            for (List<EchogramPoint> echogramPoints : editor.getSortedToBeRemovedPoints()) {
               renderPath(schoolRemovePath, echogramPoints);
            }
            // Render edit point path.
            for (List<EchogramPoint> echogramPoints : editor.getSortedEditPoints()) {
               renderPath(schoolEditPath, echogramPoints);
            }
            // Render unedited boundaries normally.
            for (List<EchogramPoint> boundary : editor.getUneditedBoundaries()) {
               renderLoop(pathBuilder, boundary);
            }
         } else {
            for (SchoolBoundaryObject boundaryObject : school.getBoundaryObjects()) {
               renderLoop(pathBuilder, boundaryObject.getBoundary());
            }
         }
      }

      private void renderPath(LineStripBuilder pathBuilder, List<EchogramPoint> echogramPoints) {
         addPoints(pathBuilder, echogramPoints);
         pathBuilder.endLineStrip();
      }

      private void renderLoop(LineStripBuilder pathBuilder, List<EchogramPoint> echogramPoints) {
         addPoints(pathBuilder, echogramPoints);
         addPoint(pathBuilder, echogramPoints.getFirst());
         pathBuilder.endLineStrip();
      }

      private void addPoints(LineStripBuilder pathBuilder, List<EchogramPoint> echogramPoints) {
         echogramPoints.forEach(echogramPoint -> {
            addPoint(pathBuilder, echogramPoint);
         });
      }

      private void addPoint(LineStripBuilder pathBuilder, EchogramPoint echogramPoint) {
         addPoint(pathBuilder, echogramPoint.pingIndex(), echogramPoint.depth());
      }

      private void addPoint(LineStripBuilder pathBuilder, PingIndex pingIndex, float depth) {
         float x = pingSettings.pingIndexToX(pingIndex);
         float y = zSettings.depthToY(depth, pingIndex);
         pathBuilder.addPoint(x, y);
      }
   }

   private final class DisplayData implements OverlayDisplayData {
      private final Path2D.Float selectedBoundaryPath = new Path2D.Float();
      private final Path2D.Float unselectedBoundaryPath = new Path2D.Float();
      private final Path2D.Float connectorPath = new Path2D.Float();
      private final Path2D.Float schoolEditPath = new Path2D.Float();
      private final Path2D.Float schoolRemovePath = new Path2D.Float();
      private final Path2D.Float selectedSchoolCenterPointPath = new Path2D.Float();
      private final Path2D.Float unselectedSchoolCenterPointPath = new Path2D.Float();
      private boolean longSelectedBoundary;
      private boolean manySchools;

      private DisplayData() {
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(Color.BLUE);
         g2d.draw(unselectedBoundaryPath);

         g2d.setStroke(longSelectedBoundary ? GuiUtils.STROKE_1 : selectedStroke);
         g2d.setColor(Color.RED);
         g2d.draw(selectedBoundaryPath);

         g2d.setStroke(GuiUtils.STROKE_2);
         g2d.setColor(Color.YELLOW);
         g2d.draw(connectorPath);

         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(Color.BLACK);
         g2d.draw(schoolEditPath);

         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(Color.CYAN);
         g2d.draw(schoolRemovePath);

         g2d.setStroke(manySchools ? GuiUtils.STROKE_1 : GuiUtils.STROKE_2);
         g2d.setColor(Color.BLACK);
         g2d.draw(unselectedSchoolCenterPointPath);
         g2d.setColor(Color.RED);
         g2d.draw(selectedSchoolCenterPointPath);
      }
   }
}
