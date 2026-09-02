package no.imr.lsss.util.phantom.echogram;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.EchogramSelection;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.EchogramWorkingMode;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.echogram.VerticalEchogramScrollBar;
import no.imr.lsss.util.phantom.echogram.overlays.BasePhantomOverlay;
import no.imr.lsss.util.phantom.echogram.overlays.SelectionPhantomOverlay;
import no.imr.lsss.util.phantom.echogram.overlays.ZoomPhantomOverlay;
import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public abstract class BasePhantomEchogramModule extends BaseOverlaidModule<BasePhantomOverlay> {
   private final PhantomEchogramSettings phantomEchogramSettings;
   private final ChangeManager echogramAreaChangeManager = new ChangeManager();
   private final VerticalEchogramScrollBar verticalEchogramScrollBar;
   private @Nullable Path lastPhantomDir;

   protected BasePhantomEchogramModule(ModuleInfo<?> moduleInfo, PhantomEchogramSettings phantomEchogramSettings) {
      super(moduleInfo);

      this.phantomEchogramSettings = phantomEchogramSettings;
      verticalEchogramScrollBar = new VerticalEchogramScrollBar(this, getLSSS(), getZSettings(), phantomEchogramSettings.getPhantomDataManager());

      getInterpretationSettings().getNavigationHistory().addZSettings(getZSettings());
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getSizeChangeManager(), newCoalescingExecListener(this::resized));

      registry.add(phantomEchogramSettings.getPhantomDataAdministrator().getChangedManager(), newCoalescingExecListener(this::initialize));

      registry.add(newCoalescingExecListener(this::echogramAreaChanged), List.of(
            getPingSettings().getChangeManager(),
            getZSettings().getZoomedChangeManager()
      ));

      Listener updateMouseEchogramPointListener = newCoalescingExecListener(this::updateMouseEchogramPoint);
      registry.add(mousePosition(), updateMouseEchogramPointListener);
      registry.add(getInterpretationSettings().mouseover().frozen(), _ -> {
         if (getMousePosition() != null) {
            updateMouseEchogramPointListener.listen();
         }
      });

      registry.add(getInterpretationSettings().getEchogramSettings().workingMode, newCoalescingExecListener(this::updateBackgroundOverlay));

      //---

      initialize();
      updateBackgroundOverlay();
      resized();
   }

   @Override
   public abstract ViewHolder<? extends BasePhantomEchogramView> getViewHolder();

   protected abstract FileParameter getPhantomDirParameter();

   private void initialize() {
      List<RawFileTransducer> transducers = phantomEchogramSettings.getPhantomDataFileSet().getRawFileConfiguration().getTransducers();
      if (!transducers.isEmpty()) {
         int channel;
         Path phantomDir = getPhantomDirParameter().getFile();
         if (!Objects.equals(phantomDir, lastPhantomDir)) {
            lastPhantomDir = phantomDir;
            channel = IntStream.rangeClosed(1, transducers.size())
                  .filter(ch -> Utils.containsIgnoringCase(transducers.get(ch - 1).getChannelId(), "max"))
                  .findFirst()
                  .orElse(transducers.size());
         } else {
            channel = Math.clamp(phantomEchogramSettings.getChannel(), 1, transducers.size());
         }
         phantomEchogramSettings.setChannel(channel);
      }
   }

   public PhantomEchogramSettings getPhantomEchogramSettings() {
      return phantomEchogramSettings;
   }

   public EchogramPingSettings getPingSettings() {
      return phantomEchogramSettings.getEchogramPingSettings();
   }

   public EchogramZSettings getZSettings() {
      return phantomEchogramSettings.getEchogramZSettings();
   }

   public ChangeManager getEchogramAreaChangeManager() {
      return echogramAreaChangeManager;
   }

   VerticalEchogramScrollBar getVerticalEchogramScrollBar() {
      return verticalEchogramScrollBar;
   }

   private void resized() {
      int width = Math.max(1, getWidth());
      int height = Math.max(1, getHeight());

      phantomEchogramSettings.setEchogramWidth(width);
      getZSettings().setHeight(height);

      echogramAreaChanged();
   }

   private void echogramAreaChanged() {
      echogramAreaChangeManager.notifyListeners();
      if (getMousePosition() != null) {
         updateMouseEchogramPoint();
      }

      updateActiveOverlayLater();
      updateToolTipText();
      repaint();
   }

   private void updateMouseEchogramPoint() {
      if (getInterpretationSettings().getDataFileSet().isEmpty()) {
         return;
      }
      Point mousePosition = getMousePosition();
      EchogramPoint echogramPoint = mousePosition != null ? imagePointToLsssEchogramPoint(mousePosition) : null;
      getInterpretationSettings().mouseover().setPos(echogramPoint);
   }

   private void updateBackgroundOverlay() {
      boolean zoom = getInterpretationSettings().getEchogramSettings().workingMode.getValue() == EchogramWorkingMode.ZOOM;
      setBackgroundOverlay(zoom ? ZoomPhantomOverlay.class : SelectionPhantomOverlay.class);
   }

   private @Nullable EchogramPoint imagePointToLsssEchogramPoint(Point mousePosition) {
      PingIndex phantomPingIndex = getPingSettings().xToContainingPingIndex(mousePosition.getX());
      if (phantomPingIndex == null) {
         return null;
      }
      PingIndex lsssPingIndex = phantomEchogramSettings.getPhantomPingIndexConverter().otherToContainingLsss(phantomPingIndex);
      if (lsssPingIndex == null) {
         return null;
      }
      return new EchogramPoint(lsssPingIndex, getZSettings().yToDepth(mousePosition.getY(), phantomPingIndex));
   }

   public abstract void doEchogramSelection(EchogramSelection echogramSelection);

   @Override
   protected void draw(Graphics2D g2d) {
      if (getPingSettings().getPingRange().isEmpty()) {
         g2d.setColor(Color.BLACK);
         g2d.fillRect(0, 0, getWidth(), getHeight());
      }
   }

   @Override
   protected boolean shouldUseOverlays() {
      return !getPingSettings().getPingRange().isEmpty();
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   public JComponent createConfigurationEditor() {
      JTabbedPane tabbedPane = new JTabbedPane();

      tabbedPane.add("Overlays", createOverlayEditor());

      ParameterEditor parameterEditor = new ParameterEditor(List.of(getZSettings().minZ, getZSettings().maxZ));
      tabbedPane.add("Depth", GuiUtils.createScrollPane(parameterEditor));

      return tabbedPane;
   }
}
