package no.imr.lsss.modules.map;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.ExtendedSurveyLine;
import no.imr.lsss.framework.MapWorkingMode;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.map.overlays.BaseMapOverlay;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.Utils;
import no.imr.tools.geo.Earth;
import no.imr.tools.geo.GeoTransform;
import no.imr.tools.geo.GeoZoom;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.listening.Listeners;
import no.imr.tools.math.MathUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.SimpleInputDialog;
import no.imr.tools.swing.TableToolTipBuilder;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Module for displaying maps and geographical information.
 * <p>
 * Reserved symbols:
 * <ul>
 * <li>Square: Bottom trawl</li>
 * <li>Triangle: Pelagic trawl</li>
 * <li>Z: CTD</li>
 * <li>C: Comment</li>
 * </ul>
 */
public final class MapModule extends BaseOverlaidModule<BaseMapOverlay> {
   private static final double PAN_STEP = 5; // in pixels
   private static final double ZOOM_FACTOR = 1.1;

   private final BooleanParameter autoZoomFromEchogram = new BooleanParameter(
         new Name("AutoZoomFromEchogram", "Auto zoom from echogram"),
         false);

   private final ObjectParameter<MapWorkingMode> workingMode = new ObjectParameter<>(
         new Name("WorkingMode", "Working mode"),
         MapWorkingMode.ZOOM, MapWorkingMode.values(),
         "Change by typing Tab or Shift + Tab");

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private int editorTabIndex;

   private GeoZoom geoZoom = new GeoZoom();

   private final UndoManager undoManager = new UndoManager();
   private final AtomicReference<@Nullable GeoZoom> debouncingOldGeoZoom = new AtomicReference<>();
   private final Listener debouncingUndoListener = Listeners.debouncing(() -> {
      GeoZoom oldGeoZoom = debouncingOldGeoZoom.getAndSet(null);
      if (oldGeoZoom != null && !oldGeoZoom.sameGeoCenterAndLatitudeExtent(geoZoom)) {
         undoManager.addEdit(new NavigationEdit(oldGeoZoom, geoZoom));
      }
   });

   private @Nullable Rectangle2D selectionGeoBox;

   private final ArgChangeManager<GeoZoom> geographicalAreaChangeManager = new ArgChangeManager<>();

   public MapModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      undoManager.setLimit(25);

      workingMode.setPersistable(false);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            autoZoomFromEchogram,
            workingMode
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getSizeChangeManager(), newCoalescingExecListener(this::resized));
      registry.add(getInterpretationSettings().getMapSettings().getBoundingBoxesChangeManager(), newCoalescingExecListener(this::resetGeoRect));
      Listener updateMouseGeoPosListener = newCoalescingExecListener(this::updateMouseGeoPos);
      registry.add(mousePosition(), updateMouseGeoPosListener);
      registry.add(getInterpretationSettings().mouseover().frozen(), _ -> {
         if (getMousePosition() != null) {
            updateMouseGeoPosListener.listen();
         }
      });
      registry.add(newCoalescingExecListener(this::zoomMapFromEchogramIfAuto), List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            autoZoomFromEchogram
      ));
      registry.add(workingMode, newCoalescingExecListener(this::updateBackgroundOverlay));

      //---

      resized();
      resetGeoRect();
      zoomMapFromEchogramIfAuto();
      updateBackgroundOverlay();
   }

   @Override
   public ViewHolder<? extends BaseOverlaidView> getViewHolder() {
      return viewHolder;
   }

   @Override
   protected String getDefaultToolTipText(Point point) {
      String format = Utils.getPrecisionString(getLatitudeExtent() / getHeight());
      GeoPoint geoPos = geoZoom.geoTransform.pixToGeo(point);

      TableToolTipBuilder toolTip = new TableToolTipBuilder()
            .addLine(Earth.formatGeoPoint(geoPos, format));

      if (selectionGeoBox != null) {
         GeoPoint geoDegreesPerMeter = Earth.getGeoDegreesPerMeter(selectionGeoBox.getCenterY());
         double deltaLat = KoronaUtils.meterToNmi(selectionGeoBox.getHeight() / geoDegreesPerMeter.getLatitude());
         double deltaLon = KoronaUtils.meterToNmi(selectionGeoBox.getWidth() / geoDegreesPerMeter.getLongitude());
         String boxFormat = Utils.getPrecisionString(KoronaUtils.meterToNmi(getLatitudeExtent() / geoDegreesPerMeter.getLatitude()) / getHeight());
         toolTip
               .addVerticalSpace()
               .addLine("Selection rectangle:")
               .addRow(" - North-South [nmi]", Utils.format(boxFormat, deltaLat))
               .addRow(" - East-West [nmi]", Utils.format(boxFormat, deltaLon))
               .addRow(" - Diagonal [nmi]", Utils.format(boxFormat, MathUtils.hypot(deltaLat, deltaLon)));
      }

      return toolTip.build();
   }

   public void setSelectionGeoBox(@Nullable Rectangle2D selectionGeoBox) {
      this.selectionGeoBox = selectionGeoBox;
      updateToolTipText();
   }

   private void resized() {
      setGeoZoom(geoZoom.withSize(getWidth(), getHeight()));
   }

   public Rectangle2D getGeoRect() {
      return geoZoom.getGeoRect();
   }

   public double getLongitudeExtent() {
      return geoZoom.getLongitudeExtent();
   }

   public double getLatitudeExtent() {
      return geoZoom.latitudeExtent;
   }

   public double getPixelsPerMeter() {
      return geoZoom.getPixelsPerMeter();
   }

   public GeoPoint getGeoCenter() {
      return geoZoom.geoCenter;
   }

   public void setGeoCenter(GeoPoint geoCenter) {
      setGeoCenterAndLatitudeExtent(geoCenter, geoZoom.latitudeExtent);
   }

   public void setGeoCenterAndLatitudeExtent(GeoPoint geoCenter, double latitudeExtent) {
      setGeoZoom(geoZoom.withGeoCenterAndLatitudeExtent(geoCenter, latitudeExtent));
   }

   private void setGeoZoom(GeoZoom newGeoZoom) {
      if (!geoZoom.equals(newGeoZoom)) {
         if (!geoZoom.sameGeoCenterAndLatitudeExtent(newGeoZoom)) {
            debouncingOldGeoZoom.compareAndSet(null, geoZoom);
            debouncingUndoListener.listen();
         }
         geoZoom = newGeoZoom;
         geographicalAreaChanged();
      }
   }

   private void setGeoZoomWithoutEdit(GeoZoom newGeoZoom) {
      geoZoom = newGeoZoom;
      geographicalAreaChanged();
   }

   public GeoTransform getGeoTransform() {
      return geoZoom.geoTransform;
   }

   private void resetGeoRect() {
      Rectangle2D geoRect = getInterpretationSettings().getMapSettings().getGeographicalBoundingBox();
      if (geoRect != null) {
         Earth.expand(geoRect, 500);
         fitGeoRect(geoRect, 1.1);
      } else {
         fitGeoRect(new Rectangle2D.Double(-180, -90, 360, 180), 1);
      }
   }

   public void fitGeoRect(Rectangle2D geoRect, double factor) {
      setGeoZoom(geoZoom.withContainingGeoRect(geoRect, factor));
   }

   public GeoZoom getGeoZoom() {
      return geoZoom;
   }

   public ArgChangeManager<GeoZoom> getGeographicalAreaChangeManager() {
      return geographicalAreaChangeManager;
   }

   private void geographicalAreaChanged() {
      geographicalAreaChangeManager.notifyListeners(geoZoom);

      updateMouseGeoPos();

      updateActiveOverlayLater();
      updateToolTipText();
      repaint();
   }

   private void updateBackgroundOverlay() {
      setBackgroundOverlay(workingMode.getValue().overlayClass);
   }

   private void pan(double x, double y) {
      Rectangle2D geoRect = getGeoRect();
      double dx = x * PAN_STEP * geoRect.getWidth() / getWidth();
      double dy = y * PAN_STEP * geoRect.getHeight() / getHeight();
      GeoPoint geoCenter = geoZoom.geoCenter;
      setGeoCenter(new GeoPoint(geoCenter.getX() + dx, geoCenter.getY() + dy));
   }

   private void zoomAroundPixPos(@Nullable Point2D zoomPixCenter, int zoomSteps) {
      if (zoomPixCenter == null) {
         zoomPixCenter = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
      }
      setGeoZoom(geoZoom.zoomAroundPixPos(zoomPixCenter, Math.pow(ZOOM_FACTOR, -zoomSteps)));
   }

   @Override
   public JComponent createConfigurationEditor() {
      JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.add("Overlays", createOverlayEditor());
      tabbedPane.add("Parameters", super.createConfigurationEditor());
      tabbedPane.setSelectedIndex(editorTabIndex);
      tabbedPane.addChangeListener(_ -> editorTabIndex = tabbedPane.getSelectedIndex());
      return tabbedPane;
   }

   private void zoomEchogram() {
      List<PingRange> pingRanges = DataUtils.getPingRangesInGeoRect(getInterpretationSettings().getDataFileSet(), getGeoRect());
      if (pingRanges.isEmpty()) {
         return;
      }
      PingRange pingRange = PingRange.of(pingRanges.getFirst().begin(), pingRanges.getLast().end());
      getInterpretationSettings().setPingRange(pingRange);
   }

   private void zoomMapFromEchogram() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      Rectangle2D geoRect = DataUtils.getGeographicalBoundingBox(dataFileSet.getPingIndices(pingRange));
      if (geoRect != null) {
         fitGeoRect(geoRect, 1.1);
      }
   }

   private void zoomMapFromEchogramIfAuto() {
      if (autoZoomFromEchogram.getBooleanValue()) {
         zoomMapFromEchogram();
      }
   }

   private void excludePings() {
      DataUtils.getPingRangesInGeoRect(getInterpretationSettings().getDataFileSet(), getGeoRect())
            .forEach(getRegionManager().getExclusionManager()::excludeRange);
   }

   private void includePings() {
      DataUtils.getPingRangesInGeoRect(getInterpretationSettings().getDataFileSet(), getGeoRect())
            .forEach(getRegionManager().getExclusionManager()::includeRange);
   }

   private void updateMouseGeoPos() {
      Point mousePosition = getMousePosition();
      GeoPoint geoPos = mousePosition != null ? geoZoom.geoTransform.pixToGeo(mousePosition) : null;
      getInterpretationSettings().mouseover().setPos(geoPos);
   }

   private final class NavigationEdit extends AbstractUndoableEdit {
      private final GeoPoint oldGeoCenter;
      private final double oldLatitudeExtent;
      private final GeoPoint newGeoCenter;
      private final double newLatitudeExtent;

      private NavigationEdit(GeoZoom oldGeoZoom, GeoZoom newGeoZoom) {
         oldGeoCenter = oldGeoZoom.geoCenter;
         oldLatitudeExtent = oldGeoZoom.latitudeExtent;
         newGeoCenter = newGeoZoom.geoCenter;
         newLatitudeExtent = newGeoZoom.latitudeExtent;
      }

      @Override
      public void undo() {
         super.undo();
         setGeoZoomWithoutEdit(geoZoom.withGeoCenterAndLatitudeExtent(oldGeoCenter, oldLatitudeExtent));
      }

      @Override
      public void redo() {
         super.redo();
         setGeoZoomWithoutEdit(geoZoom.withGeoCenterAndLatitudeExtent(newGeoCenter, newLatitudeExtent));
      }
   }

   private static final class View extends BaseOverlaidView {
      private final MapModule module;
      private float extendedSurveyLineHours = 1;

      private View(MapModule module) {
         super(module);

         this.module = module;

         JComponent component = getComponent();
         component.addMouseWheelListener(this::mouseWheelMoved);
      }

      @Override
      protected KeyListener getKeyListener() {
         return new MapKeyListener();
      }

      @Override
      protected JPopupMenu getDefaultPopupMenu(Point point) {
         JPopupMenu popupMenu = super.getDefaultPopupMenu(point);

         popupMenu.addSeparator();

         JMenuItem resetViewItem = MiscIcons.HOME.on(popupMenu.add("Reset geographical area"));
         resetViewItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
         resetViewItem.setToolTipText("Reset to default geographical area");
         resetViewItem.addActionListener(_ -> module.resetGeoRect());

         JMenuItem zoomEchogramItem = popupMenu.add("Zoom echogram from map");
         zoomEchogramItem.setToolTipText("Zoom echogram to currently visible survey path");
         zoomEchogramItem.addActionListener(_ -> module.zoomEchogram());

         JMenuItem zoomMapItem = popupMenu.add("Zoom map from echogram");
         zoomMapItem.setToolTipText("Zoom map to match currently visible part of echogram");
         zoomMapItem.addActionListener(_ -> module.zoomMapFromEchogram());

         popupMenu.addSeparator();

         JMenuItem excludeItem = LsssIcons.EXCLUDE.on(popupMenu.add("Exclude visible pings"));
         excludeItem.setToolTipText("Exclude all pings visible in map");
         excludeItem.addActionListener(_ -> module.excludePings());

         JMenuItem includeItem = popupMenu.add("Include visible pings");
         includeItem.setToolTipText("Include all pings visible in map");
         includeItem.addActionListener(_ -> module.includePings());

         popupMenu.addSeparator();

         JMenuItem undoItem = MiscIcons.ARROW_LEFT.on(popupMenu.add("Go to previous map location"));
         undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0));
         if (module.undoManager.canUndo()) {
            undoItem.addActionListener(_ -> module.undoManager.undo());
         } else {
            undoItem.setEnabled(false);
         }

         JMenuItem redoItem = MiscIcons.ARROW_RIGHT.on(popupMenu.add("Go to next map location"));
         redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
         if (module.undoManager.canRedo()) {
            redoItem.addActionListener(_ -> module.undoManager.redo());
         } else {
            redoItem.setEnabled(false);
         }

         popupMenu.addSeparator();

         ExtendedSurveyLine extendedSurveyLine = module.getInterpretationSettings().getMapSettings().getExtendedSurveyLine();

         JMenuItem extendSurveyLineItem = popupMenu.add("Extend survey line...");
         extendSurveyLineItem.setToolTipText("Show extended survey line without loading raw data");
         extendSurveyLineItem.addActionListener(_ -> {
            new SimpleInputDialog<>("Extended survey line", "Additional time backwards [hours]", Utils.toString(extendedSurveyLineHours), Float::parseFloat)
                  .show(getComponent())
                  .ifPresent(hours -> {
                     extendedSurveyLineHours = hours;
                     ProgressView progressView = new ProgressView("Reading additional index files", 1000)
                           .mainProgressAsPercentage();
                     new WorkerDialog(getComponent(), progressView.getComponent())
                           .start(asyncHandle -> {
                              extendedSurveyLine.addHours(hours, progressView.getMainProgressHandler(), asyncHandle);
                           });
                  });
         });

         JMenuItem removeExtendedSurveyLineItem = popupMenu.add("Remove extended survey line");
         removeExtendedSurveyLineItem.setEnabled(!extendedSurveyLine.getExtendedPingIndices().isEmpty());
         removeExtendedSurveyLineItem.addActionListener(_ -> {
            extendedSurveyLine.reset();
         });

         return popupMenu;
      }

      private void mouseWheelMoved(MouseWheelEvent e) {
         if (e.getModifiersEx() == 0) {
            module.zoomAroundPixPos(e.getPoint(), e.getWheelRotation());
         }
      }

      private final class MapKeyListener extends KeyAdapter {
         private MapKeyListener() {
         }

         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_HOME -> {
                  if (e.getModifiersEx() == 0) {
                     module.resetGeoRect();
                  }
               }
               case KeyEvent.VK_TAB -> {
                  if (e.getModifiersEx() == 0 || e.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK) {
                     module.workingMode.shiftValue(e.isShiftDown() ? -1 : 1);
                  }
               }
               case KeyEvent.VK_LEFT -> {
                  if (e.getModifiersEx() == 0) {
                     module.pan(-1, 0);
                  }
               }
               case KeyEvent.VK_RIGHT -> {
                  if (e.getModifiersEx() == 0) {
                     module.pan(1, 0);
                  }
               }
               case KeyEvent.VK_UP -> {
                  if (e.getModifiersEx() == 0) {
                     module.pan(0, 1);
                  }
               }
               case KeyEvent.VK_DOWN -> {
                  if (e.getModifiersEx() == 0) {
                     module.pan(0, -1);
                  }
               }
               case KeyEvent.VK_I, KeyEvent.VK_PLUS, KeyEvent.VK_ADD -> {
                  if (e.getModifiersEx() == 0) {
                     module.zoomAroundPixPos(module.getMousePosition(), 1);
                  }
               }
               case KeyEvent.VK_O, KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT -> {
                  if (e.getModifiersEx() == 0) {
                     module.zoomAroundPixPos(module.getMousePosition(), -1);
                  }
               }
               case KeyEvent.VK_B -> {
                  if (e.getModifiersEx() == 0) {
                     if (module.undoManager.canUndo()) {
                        module.undoManager.undo();
                     }
                  }
               }
               case KeyEvent.VK_N -> {
                  if (e.getModifiersEx() == 0) {
                     if (module.undoManager.canRedo()) {
                        module.undoManager.redo();
                     }
                  }
               }
               default -> {
               }
            }
         }
      }
   }
}
