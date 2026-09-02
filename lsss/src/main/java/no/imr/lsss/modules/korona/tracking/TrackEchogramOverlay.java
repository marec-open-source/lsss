package no.imr.lsss.modules.korona.tracking;

import com.google.common.base.Suppliers;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.viewer.variables.raw.TsuVariable;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.lsss.resources.LsssCursors;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.Function1D;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.undo.UndoManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class TrackEchogramOverlay extends BaseEchogramOverlay {
   private static final Color TRACK_FILL_COLOR = new Color(64, 64, 64, 128);
   private static final BasicStroke IGNORE_ANGLES_CENTER_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 2}, 0);

   private final Supplier<TrackInfoModule> trackInfoModule = moduleSupplier(TrackInfoModule.class);
   private final Supplier<EchogramTrackData> echogramTrackData = Suppliers.memoize(() -> trackInfoModule.get().getEchogramTrackData(getEchogramModule()));
   private final Listener recomputeListener = createRecomputeListener();

   public enum WhichTracks {
      ALL, ACCEPTED, REJECTED
   }

   private final BooleanParameter showExtent = new BooleanParameter(
         new Name("ShowExtent", "Show extent"),
         true,
         "Shows vertical extent of tracks");

   private final BooleanParameter showLabels = new BooleanParameter(
         new Name("ShowLabels", "Show labels"),
         false,
         "Display track labels");

   private final ObjectParameter<WhichTracks> whichTracks = new ObjectParameter<>(
         new Name("WhichTracks", "Which tracks"),
         WhichTracks.ACCEPTED, WhichTracks.values());

   private static boolean editMode;
   private static boolean editModeDraw = true;
   private volatile @Nullable TrackId activeTrackId;
   private volatile @Nullable Editing editing;

   public TrackEchogramOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            showExtent,
            showLabels,
            whichTracks
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(recomputeListener, List.of(
            echogramTrackData.get().getTrackDataChangeManager(),
            trackInfoModule.get().getTrackSelection().getChangeManager()
      ));
      registry.add(getParameters(), recomputeListener);
   }

   private void setActiveTrackId(@Nullable TrackId trackId) {
      if (activeTrackId == trackId) {
         return;
      }
      activeTrackId = trackId;
      repaint();
   }

   @Override
   public boolean readyToTakeFocus() {
      TrackId activeTrackId = this.activeTrackId;
      return editMode && activeTrackId != null && trackInfoModule.get().isTrackWritable(activeTrackId);
   }

   @Override
   public void onActivate() {
      updateCursor();
   }

   @Override
   public void onDeactivate() {
      editing = null;
      recomputeListener.listen();
   }

   private void updateCursor() {
      setCursor(editModeDraw ? LsssCursors.EDIT : LsssCursors.ERASE);
   }

   @Override
   public @Nullable JPopupMenu getPopupMenu(Point point) {
      Context context = createContext();
      if (context == null) {
         return null;
      }

      JPopupMenu popupMenu = new JPopupMenu();

      popupMenu.add(trackInfoModule.get().getTrackLabelling().createLabelsMenu());

      popupMenu.addSeparator(); // --------------------------------------------------------------------------------

      JMenuItem mergeItem = popupMenu.add("Merge selected tracks");
      mergeItem.setMnemonic(KeyEvent.VK_M);
      mergeItem.setEnabled(context.selectedTrackIds.size() > 1);
      mergeItem.addActionListener(_ -> context.merge());

      JMenuItem splitItem = popupMenu.add("Split this track");
      splitItem.setMnemonic(KeyEvent.VK_S);
      splitItem.setEnabled(!context.pingIndex.equals(context.trackPingRange.begin()));
      splitItem.addActionListener(_ -> context.split());

      JMenuItem deleteItem = MiscIcons.DELETE.on(popupMenu.add("Delete selected tracks"));
      deleteItem.setMnemonic(KeyEvent.VK_D);
      deleteItem.setEnabled(!context.selectedTrackIds.isEmpty());
      deleteItem.addActionListener(_ -> context.delete());

      popupMenu.addSeparator(); // --------------------------------------------------------------------------------

      JMenuItem joinItem = popupMenu.add("Join selected tracks");
      joinItem.setMnemonic(KeyEvent.VK_J);
      joinItem.setEnabled(context.selectedTrackIds.size() > 1);
      joinItem.addActionListener(_ -> context.join());

      JMenuItem extendItem = popupMenu.add("Extend this track (to the " + (context.extendLeft ? "left" : "right") + ") (Min TSU = " + context.minTSU + " [dB])");
      extendItem.setMnemonic(KeyEvent.VK_E);
      extendItem.addActionListener(_ -> context.extend());

      JMenuItem drawingItem = MiscIcons.checkBox(editMode).on(popupMenu.add("Semi-automatic drawing"));
      drawingItem.setMnemonic(KeyEvent.VK_I);
      drawingItem.addActionListener(_ -> {
         editMode = !editMode;
         editModeDraw = true;
      });

      popupMenu.addSeparator(); // --------------------------------------------------------------------------------

      UndoManager undoManager = trackInfoModule.get().getTrackEditing().getUndoManager();

      JMenuItem undoItem = MiscIcons.UNDO.on(popupMenu.add("Undo track edit"));
      undoItem.setMnemonic(KeyEvent.VK_U);
      undoItem.setEnabled(undoManager.canUndo());
      undoItem.addActionListener(_ -> context.undo());

      JMenuItem redoItem = MiscIcons.REDO.on(popupMenu.add("Redo track edit"));
      redoItem.setMnemonic(KeyEvent.VK_R);
      redoItem.setEnabled(undoManager.canRedo());
      redoItem.addActionListener(_ -> context.redo());

      popupMenu.addSeparator(); // --------------------------------------------------------------------------------

      JMenuItem thisTrackItem = popupMenu.add("Create LSSS region for this track");
      thisTrackItem.addActionListener(_ -> trackInfoModule.get().createSchoolsAndSelect(Stream.of(context.trackId)));

      popupMenu.add(TrackUtils.menuItemCreateRegionsForSelected(trackInfoModule.get()));

      popupMenu.add(TrackUtils.createMenuItemRegionsForPingRange(trackInfoModule.get()));

      popupMenu.addSeparator(); // --------------------------------------------------------------------------------

      popupMenu.add(createConfigureMenuItem());

      return popupMenu;
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_ESCAPE -> {
            editing = null;
            recomputeListener.listen();
         }
         case KeyEvent.VK_SPACE -> {
            editModeDraw = !editModeDraw;
            updateCursor();
            Editing editing = this.editing;
            Point mousePosition = getEchogramModule().getMousePosition();
            if (editing != null && mousePosition != null) {
               editing.drag(mousePosition);
            }
         }
         default -> {
            Context context = createContext();
            return context != null && contextKeyPressed(context, keyEvent);
         }
      }
      return true;
   }

   private boolean contextKeyPressed(Context context, KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_D -> context.delete();
         case KeyEvent.VK_E -> context.extend();
         case KeyEvent.VK_J -> context.join();
         case KeyEvent.VK_M -> context.merge();
         case KeyEvent.VK_R -> context.redo();
         case KeyEvent.VK_S -> context.split();
         case KeyEvent.VK_U -> context.undo();
         default -> {
            return false;
         }
      }
      return true;
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      TrackId activeTrackId = this.activeTrackId;
      if (activeTrackId == null) {
         return;
      }
      editing = new Editing(activeTrackId, mouseEvent.getPoint());
      getEchogramModule().setActiveOverlayLocked(true);
      recomputeListener.listen();
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      Editing editing = this.editing;
      if (editing == null) {
         return;
      }
      this.editing = null;
      getEchogramModule().setActiveOverlayLocked(false);
      if (editing.trackEditingDetection.didEdit()) {
         trackInfoModule.get().getTrackEditing().apply(editing.trackEditingDetection);
      }
      recomputeListener.listen();
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      TrackId activeTrackId = this.activeTrackId;
      if (activeTrackId == null) {
         return;
      }
      if (mouseEvent.isControlDown()) {
         trackInfoModule.get().getTrackSelection().toggle(List.of(activeTrackId));
      } else if (mouseEvent.isShiftDown()) {
         trackInfoModule.get().getTrackSelection().add(List.of(activeTrackId));
      } else {
         trackInfoModule.get().getTrackSelection().replace(List.of(activeTrackId));
      }
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      Editing editing = this.editing;
      if (editing == null) {
         return;
      }
      editing.drag(mouseEvent.getPoint());
   }

   private void showErrorDialog(List<String> errors) {
      if (!errors.isEmpty() && getInterpretationSettings().isInteractiveMode()) {
         String message = String.join("\n", errors);
         JOptionPane.showMessageDialog(getEchogramModule().getComponent(), message, "Error", JOptionPane.ERROR_MESSAGE);
      }
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      getEchogramModule().updateActiveOverlayLater();

      TrackId activeTrackId = this.activeTrackId;
      if (activeTrackId != null && !isDisplayable(activeTrackId)) {
         this.activeTrackId = null;
      }

      Map<TrackId, EchogramTrackData.TrackData> trackDataMap = echogramTrackData.get().getTrackData();
      if (trackDataMap.isEmpty()) {
         return null;
      }

      Editing editing = this.editing;
      EchogramTrackData.TrackData editTrackData = editing != null ? editing.toTrackData() : null;

      Set<TrackId> selectedTrackIds = trackInfoModule.get().getTrackSelection().getSelectedTrackIds();
      List<EchogramTrackData.TrackData> selectedTrackData = new ArrayList<>();
      List<EchogramTrackData.TrackData> unselectedTrackData = new ArrayList<>();
      trackDataMap.forEach((trackId, trackData) -> {
         if (editTrackData != null && editTrackData.trackId().equals(trackId)) {
            trackData = editTrackData;
         }
         if (isDisplayable(trackId)) {
            if (selectedTrackIds.contains(trackData.trackId())) {
               selectedTrackData.add(trackData);
            } else {
               unselectedTrackData.add(trackData);
            }
         }
      });
      return transformed(new DisplayData(selectedTrackData, unselectedTrackData, editTrackData));
   }

   private boolean isDisplayable(TrackId trackId) {
      return switch (whichTracks.getValue()) {
         case ALL -> true;
         case ACCEPTED -> isAccepted(trackId);
         case REJECTED -> !isAccepted(trackId);
      };
   }

   private boolean isAccepted(TrackId trackId) {
      return trackInfoModule.get().getValidIds().contains(trackId);
   }

   private TrackEchogramOverlay.@Nullable Context createContext() {
      TrackId activeTrackId = this.activeTrackId;
      if (activeTrackId == null) {
         return null;
      }
      Point mousePosition = getEchogramModule().getMousePosition();
      if (mousePosition == null) {
         return null;
      }
      PingIndex pingIndex = getPingSettings().xToContainingPingIndex(mousePosition.x);
      if (pingIndex == null) {
         return null;
      }
      TrackInfo trackInfo = trackInfoModule.get().getTrackInfos().get(activeTrackId);
      if (trackInfo == null) {
         return null;
      }
      return new Context(trackInfo, pingIndex, mousePosition);
   }

   private final class Context {
      private final TrackId trackId;
      private final PingRange trackPingRange;
      private final PingIndex pingIndex;
      private final boolean extendLeft;
      private final Set<TrackId> selectedTrackIds = trackInfoModule.get().getTrackSelection().getSelectedTrackIds();
      private final boolean selectedTracksIdsReadOnly = selectedTrackIds.stream().anyMatch(trackInfoModule.get()::isTrackReadOnly);
      private final float minTSU = getInterpretationSettings().getColorConverterContainer().getContinuousVariable(TsuVariable.class).getSettings().getRange().min();
      private final UndoManager undoManager = trackInfoModule.get().getTrackEditing().getUndoManager();

      private Context(TrackInfo trackInfo, PingIndex pingIndex, Point point) {
         trackId = trackInfo.trackId();
         trackPingRange = trackInfo.pingRange();
         this.pingIndex = pingIndex;

         double distBegin = Math.abs(getPingSettings().pingIndexToX(trackPingRange.begin()) - point.getX());
         double distEnd = Math.abs(getPingSettings().pingIndexToX(trackPingRange.end()) - point.getX());
         extendLeft = distBegin < distEnd;
      }

      private void delete() {
         if (selectedTracksIdsReadOnly) {
            showErrorDialog(List.of("Cannot edit read-only tracks"));
            return;
         }
         trackInfoModule.get().getTrackEditing().delete(selectedTrackIds);
      }

      private void extend() {
         if (trackInfoModule.get().isTrackReadOnly(trackId)) {
            showErrorDialog(List.of("Cannot edit read-only tracks"));
            return;
         }
         trackInfoModule.get().getTrackEditing().extend(trackId, extendLeft, minTSU);
      }

      private void join() {
         if (selectedTracksIdsReadOnly) {
            showErrorDialog(List.of("Cannot edit read-only tracks"));
            return;
         }
         List<String> errors = trackInfoModule.get().getTrackEditing().combine(selectedTrackIds, true);
         showErrorDialog(errors);
      }

      private void merge() {
         if (selectedTracksIdsReadOnly) {
            showErrorDialog(List.of("Cannot edit read-only tracks"));
            return;
         }
         List<String> errors = trackInfoModule.get().getTrackEditing().combine(selectedTrackIds, false);
         showErrorDialog(errors);
      }

      private void redo() {
         if (undoManager.canRedo()) {
            undoManager.redo();
         }
      }

      private void split() {
         if (trackInfoModule.get().isTrackReadOnly(trackId)) {
            showErrorDialog(List.of("Cannot edit read-only tracks"));
            return;
         }
         trackInfoModule.get().getTrackEditing().split(trackId, pingIndex);
      }

      private void undo() {
         if (undoManager.canUndo()) {
            undoManager.undo();
         }
      }
   }

   private final class Editing {
      private final TrackId trackId;
      private final TrackEditingDetection trackEditingDetection;
      private Point lastPoint;
      private boolean lastEditModeDraw = editModeDraw;

      private Editing(TrackId trackId, Point point) {
         this.trackId = trackId;
         trackEditingDetection = new TrackEditingDetection(trackInfoModule.get(), Set.of(trackId));
         lastPoint = point;
      }

      private void drag(Point point) {
         if (lastPoint.equals(point) && lastEditModeDraw == editModeDraw) {
            return;
         }
         PingIndex p1 = getPingSettings().xToClampedContainingPingIndex(lastPoint.x);
         PingIndex p2 = getPingSettings().xToClampedContainingPingIndex(point.x);
         DoubleUnaryOperator xToY = Function1D.linear(lastPoint, point);
         getInterpretationSettings().getDataFileSet().getPingIndices(PingRange.ofUnsorted(p1, p2)).forEach(pingIndex -> {
            float x = getPingSettings().pingIndexToX(pingIndex);
            float y = (float) xToY.applyAsDouble(x);
            edit(pingIndex, getZSettings().yToDepth(y, pingIndex));
         });
         edit(p2, getZSettings().yToDepth(point.y, p2));
         recomputeListener.listen();
         lastPoint = point;
         lastEditModeDraw = editModeDraw;
      }

      private void edit(PingIndex pingIndex, float depth) {
         if (editModeDraw) {
            TrackBorder trackBorder = trackEditingDetection.getTrackBorders().get(pingIndex);
            if (trackBorder != null && trackBorder.useAngles() && trackBorder.depthRange().contains(depth)) {
               return;
            }
            trackEditingDetection.extend(pingIndex, depth, Float.NEGATIVE_INFINITY);
         } else {
            trackEditingDetection.delete(pingIndex);
         }
      }

      private EchogramTrackData.@Nullable TrackData toTrackData() {
         Map<TrackId, EchogramTrackData.TrackData> trackDataMap = echogramTrackData.get().createTrackData((ping, _) -> {
            TrackBorder trackBorder = trackEditingDetection.getTrackBorders().get(ping.getPingIndex());
            return trackBorder != null ? Stream.of(trackBorder.withId(trackId)) : Stream.empty();
         });
         return trackDataMap.get(trackId);
      }
   }

   private final class DisplayData implements OverlayDisplayData {
      private final List<EchogramTrackData.TrackData> selectedTrackData;
      private final List<EchogramTrackData.TrackData> unselectedTrackData;
      private final EchogramTrackData.@Nullable TrackData editTrackData;

      private DisplayData(List<EchogramTrackData.TrackData> selectedTrackData, List<EchogramTrackData.TrackData> unselectedTrackData, EchogramTrackData.@Nullable TrackData editTrackData) {
         this.selectedTrackData = selectedTrackData;
         this.unselectedTrackData = unselectedTrackData;
         this.editTrackData = editTrackData;
      }

      @Override
      public void draw(Graphics2D g2d) {
         if (showExtent.getBooleanValue()) {
            g2d.setColor(TRACK_FILL_COLOR);
            unselectedTrackData.forEach(trackData -> g2d.fill(trackData.extent()));

            g2d.setColor(Color.DARK_GRAY);
            unselectedTrackData.forEach(trackData -> g2d.draw(trackData.extent()));

            g2d.setColor(TRACK_FILL_COLOR);
            selectedTrackData.forEach(trackData -> g2d.fill(trackData.extent()));

            g2d.setColor(Color.RED);
            selectedTrackData.forEach(trackData -> g2d.draw(trackData.extent()));
         }
         g2d.setColor(Color.BLACK);
         unselectedTrackData.forEach(trackData -> g2d.draw(trackData.center()));

         g2d.setColor(Color.RED);
         selectedTrackData.forEach(trackData -> g2d.draw(trackData.center()));

         g2d.setColor(Color.WHITE);
         g2d.setStroke(IGNORE_ANGLES_CENTER_STROKE);
         unselectedTrackData.forEach(trackData -> g2d.draw(trackData.ignoreAnglesCenter()));
         selectedTrackData.forEach(trackData -> g2d.draw(trackData.ignoreAnglesCenter()));

         drawActiveTrack(g2d);
      }

      private void drawActiveTrack(Graphics2D g2d) {
         TrackId activeTrackId = TrackEchogramOverlay.this.activeTrackId;
         if (activeTrackId == null) {
            return;
         }
         EchogramTrackData.TrackData activeTrackData = editTrackData;
         if (activeTrackData == null) {
            activeTrackData = echogramTrackData.get().getTrackData().get(activeTrackId);
         }
         if (activeTrackData == null) {
            return;
         }

         boolean selected = trackInfoModule.get().getTrackSelection().getSelectedTrackIds().contains(activeTrackId);

         g2d.setStroke(GuiUtils.STROKE_2);
         if (showExtent.getBooleanValue()) {
            g2d.setColor(selected ? Color.RED : Color.DARK_GRAY);
            g2d.draw(activeTrackData.extent());
         }
         g2d.setColor(selected ? Color.RED : Color.BLACK);
         g2d.draw(activeTrackData.center());

         g2d.setColor(Color.WHITE);
         g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 2}, 0));
         g2d.draw(activeTrackData.ignoreAnglesCenter());
      }

      @Override
      public void drawText(Graphics2D g2d) {
         if (showLabels.getBooleanValue()) {
            drawTrackDataText(g2d, unselectedTrackData);
            drawTrackDataText(g2d, selectedTrackData);
         }
      }

      private static void drawTrackDataText(Graphics2D g2d, List<EchogramTrackData.TrackData> trackDatas) {
         for (EchogramTrackData.TrackData trackData : trackDatas) {
            GuiText text = trackData.labelText();
            if (text != null) {
               text.draw(g2d);
            }
         }
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         setActiveTrackId(Stream.concat(selectedTrackData.stream(), unselectedTrackData.stream())
               .filter(trackData -> {
                  return showExtent.getBooleanValue()
                        ? trackData.extent().intersects(rectangle)
                        : trackData.center().intersects(rectangle);
               })
               .map(EchogramTrackData.TrackData::trackId)
               .findFirst()
               .orElse(null));
         return activeTrackId != null;
      }
   }
}
