package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.mask.GrowEngine;
import no.imr.korona.data.util.mask.MaskUtils;
import no.imr.korona.region.School;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.EchogramSettings;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.resources.LsssCursors;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;

/**
 * Background overlay for adding layer boundaries.
 */
public final class RegionAddOverlay extends BaseEchogramOverlay {
   private static final float MIN_SCHOOL_PIXEL_SIZE = 5;

   private @Nullable EchogramPoint schoolStartPoint;

   public enum AddMode {
      HORIZONTAL_BOUNDARY,
      VERTICAL_BOUNDARY,
      SCHOOL
   }

   private final EchogramSettings echogramSettings;

   public RegionAddOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      echogramSettings = getInterpretationSettings().getEchogramSettings();
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(echogramSettings.addSubMode,
            GuiListeners.coalescingLater(this::onSubModeChange));
   }

   @Override
   public boolean isBackgroundOverlay() {
      return true;
   }

   @Override
   public void onActivate() {
      updateCursor();
   }

   @Override
   public void onDeactivate() {
      setSchoolStartPoint(null);
   }

   private void onSubModeChange() {
      setSchoolStartPoint(null);
      updateCursor();
   }

   private boolean isPenetrating() {
      return (getEchogramModule().getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
   }

   private boolean mouseIsInReadOnly() {
      Point mousePosition = getEchogramModule().getMousePosition();
      if (mousePosition == null) {
         return true;
      }
      EchogramPoint echogramPoint = getEchogramModule().imagePointToEchogramPoint(mousePosition);
      return echogramPoint == null || getRegionManager().isReadOnlyIncludingEnd(echogramPoint.pingIndex());
   }

   private void updateCursor() {
      if (mouseIsInReadOnly()) {
         setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
         return;
      }
      Cursor cursor = switch (echogramSettings.addSubMode.getValue()) {
         case HORIZONTAL_BOUNDARY -> isPenetrating() ? LsssCursors.ADD_HORIZONTAL_MAX : LsssCursors.ADD_HORIZONTAL;
         case VERTICAL_BOUNDARY -> isPenetrating() ? LsssCursors.ADD_VERTICAL_MAX : LsssCursors.ADD_VERTICAL;
         case SCHOOL -> LsssCursors.ADD_BOX;
      };
      setCursor(cursor);
   }

   @Override
   public void mouseMoved(MouseEvent mouseEvent) {
      updateCursor();
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      EchogramPoint echogramPoint = getEchogramModule().imagePointToEchogramPoint(mouseEvent.getPoint());
      if (echogramPoint == null) {
         return;
      }

      if (!getRegionManager().isReadOnlyIncludingEnd(echogramPoint.pingIndex())) {
         switch (echogramSettings.addSubMode.getValue()) {
            case HORIZONTAL_BOUNDARY -> {
               if (isPenetrating()) {
                  getRegionManager().addHorizontalDivider(echogramPoint, getZSettings().getDepthTransform());
               } else {
                  getRegionManager().getLayerManager().addCurveBoundary(echogramPoint, getZSettings().getDepthTransform());
               }
            }
            case VERTICAL_BOUNDARY -> {
               if (isPenetrating()) {
                  getRegionManager().addVerticalDivider(echogramPoint.pingIndex());
               } else {
                  getRegionManager().getLayerManager().addVerticalBoundary(echogramPoint);
               }
            }
            case SCHOOL -> {
               JPanel panel = new JPanel(new BorderLayout());
               panel.add(new JLabel("Growing school..."));
               JLabel pingCountLabel = new JLabel("");
               panel.add(pingCountLabel, BorderLayout.SOUTH);
               new WorkerDialog(getEchogramModule().getComponent(), panel)
                     .start(asyncHandle -> {
                        float nextDepth = getZSettings().yToDepth(mouseEvent.getY() + 1, echogramPoint.pingIndex());
                        // NB: nextDepth < echogramPoint.getDepth() if seabed mounted
                        FloatRange depthRange = FloatRange.ofUnsorted(echogramPoint.depth(), nextDepth);
                        PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
                        Function<PingIndex, FloatRangeSet> depthRangeExtractor = MaskUtils.schoolCandidateDepthRangeExtractor(
                              getLSSS().getRegionManager(),
                              getLSSS().getInterpretationSettings().getDataFileSet(),
                              getLSSS().getInterpretationSettings().getChannel()
                        );
                        new GrowEngine(getLSSS().getInterpretationSettings().getDataFileSet(), depthRangeExtractor)
                              .setUsablePings(pingIndex -> {
                                 return pingRange.contains(pingIndex) && !getRegionManager().isReadOnly(pingIndex);
                              })
                              .addSeed(echogramPoint.pingIndex(), depthRange)
                              .setPingCountListener(GuiListeners.coalescingLater(pingCount -> pingCountLabel.setText(pingCount + " pings")))
                              .growSchoolMask(asyncHandle)
                              .ifPresent(schoolMask -> {
                                 schoolMask = MaskUtils.fillHoles(schoolMask, getInterpretationSettings().getDataFileSet());
                                 schoolMask = MaskUtils.smooth(schoolMask, getConfigurationManager().getSurveyMiscConf().schoolGrowSmoothing.getFloatValue(),
                                       getPingSettings(), getZSettings());
                                 School school = getRegionManager().addSchool(schoolMask);
                                 if (school != null) {
                                    getRegionManager().replaceSelectedRegions(school);
                                 }
                              });
                     });
            }
         }
      }

      echogramSettings.useDefaultIfNotSticky();
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      EchogramPoint echogramPoint = getEchogramModule().imagePointToEchogramPoint(mouseEvent.getPoint());
      if (echogramPoint == null || getRegionManager().isReadOnlyIncludingEnd(echogramPoint.pingIndex())) {
         setSchoolStartPoint(null);
         return;
      }

      switch (echogramSettings.addSubMode.getValue()) {
         case SCHOOL -> {
            setSchoolStartPoint(echogramPoint);
         }
         case HORIZONTAL_BOUNDARY, VERTICAL_BOUNDARY -> {
         }
      }
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      recompute();
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return switch (echogramSettings.addSubMode.getValue()) {
         case SCHOOL -> {
            Point mousePosition = getEchogramModule().getMousePosition();
            if (schoolStartPoint != null && mousePosition != null) {
               Point2D p1 = getEchogramModule().echogramPointToImagePoint(schoolStartPoint);
               EchogramPoint echogramPoint = getEchogramModule().imagePointToEchogramPoint(mousePosition);
               PingIndex clampedEndIndex = getRegionManager().toWritablePingIndex(echogramPoint, schoolStartPoint.pingIndex(), getInterpretationSettings().getPingRange());
               if (echogramPoint != null && clampedEndIndex != null) {
                  Point2D p2 = getEchogramModule().echogramPointToImagePoint(new EchogramPoint(clampedEndIndex, echogramPoint.depth()));
                  Rectangle2D.Float schoolBox = new Rectangle2D.Float();
                  schoolBox.setFrameFromDiagonal(p1, p2);
                  yield new DisplayData(schoolBox);
               }
            }
            yield null;
         }
         case HORIZONTAL_BOUNDARY, VERTICAL_BOUNDARY -> {
            yield null;
         }
      };
   }

   private void setSchoolStartPoint(@Nullable EchogramPoint schoolStartPoint) {
      this.schoolStartPoint = schoolStartPoint;
      recompute();
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      PingIndex pingIndex = getPingSettings().xToContainingPingIndex(mouseEvent.getX());
      if (pingIndex == null) {
         return;
      }

      switch (echogramSettings.addSubMode.getValue()) {
         case SCHOOL -> {
            setDisplayData(null);
            if (schoolStartPoint == null) {
               return;
            }
            Point2D a = getEchogramModule().echogramPointToImagePoint(schoolStartPoint);
            PingIndex clampedIndex = getRegionManager().toWritablePingIndex(getEchogramModule().imagePointToEchogramPoint(mouseEvent.getPoint()),
                  schoolStartPoint.pingIndex(), getInterpretationSettings().getPingRange());
            if (clampedIndex != null) {
               float depth = getZSettings().yToClampedDepth(mouseEvent.getY(), clampedIndex);
               EchogramPoint echogramPoint = new EchogramPoint(clampedIndex, depth);
               Point2D b = getEchogramModule().echogramPointToImagePoint(new EchogramPoint(clampedIndex, echogramPoint.depth()));
               if (Math.abs(b.getX() - a.getX()) >= MIN_SCHOOL_PIXEL_SIZE && Math.abs(b.getY() - a.getY()) >= MIN_SCHOOL_PIXEL_SIZE) {
                  School school = getRegionManager().addSchool(schoolStartPoint, echogramPoint, getZSettings().getDepthTransform());
                  if (school != null) {
                     getRegionManager().replaceSelectedRegions(school);
                     echogramSettings.useDefaultIfNotSticky();
                  }
               }
            }
            setSchoolStartPoint(null);
         }
         case HORIZONTAL_BOUNDARY, VERTICAL_BOUNDARY -> {
         }
      }
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_ESCAPE -> {
            if (echogramSettings.addSubMode.getValue() == AddMode.SCHOOL && schoolStartPoint != null) {
               setSchoolStartPoint(null);
               echogramSettings.useDefaultIfNotSticky();
            } else {
               echogramSettings.useDefault();
            }
         }
         case KeyEvent.VK_SPACE -> {
            echogramSettings.addSubMode.shiftValue(keyEvent.isShiftDown() ? -1 : 1);
         }
         case KeyEvent.VK_CONTROL -> {
            updateCursor();
         }
         default -> {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean keyReleased(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_CONTROL -> {
            updateCursor();
         }
         default -> {
            return false;
         }
      }
      return true;
   }

   private record DisplayData(Rectangle2D.Float schoolBox) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(Color.BLUE);
         g2d.draw(schoolBox);
      }
   }
}
