package no.imr.lsss.modules.echogram;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.region.CurveBoundary;
import no.imr.korona.region.ExclusionManager;
import no.imr.korona.region.Region;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.DataLoadingMode;
import no.imr.lsss.framework.EchogramSettings;
import no.imr.lsss.framework.InterpretationZSettings;
import no.imr.lsss.framework.config.survey.data.DataType;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.comment.CommentDataModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.lsss.modules.echogram.overlays.RegionEditOverlay;
import no.imr.lsss.modules.integration.IntegrationArea;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.resources.LsssIcons;
import no.imr.lsss.util.LsssUtils;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpID;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.RangeSet;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SimpleInputDialog;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;

/**
 * Displays an echogram image with overlays.
 */
public abstract sealed class EchogramModule extends BaseOverlaidModule<BaseEchogramOverlay> permits BottomEchogramModule, PelagicEchogramModule {
   private final InterpretationZSettings zSettings;
   private final VerticalEchogramScrollBar verticalEchogramScrollBar;
   private final ToolTipGenerator toolTipGenerator;

   private final ListenableProperty<EchogramArea> echogramArea = new ListenableProperty<>(new EchogramArea(PingRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE, 1, 1));

   private int editorTabIndex;

   protected EchogramModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, InterpretationZSettings zSettings) {
      super(moduleInfo);

      this.zSettings = zSettings;
      verticalEchogramScrollBar = new VerticalEchogramScrollBar(this, getLSSS(), zSettings, getLSSS().getDataManager());
      toolTipGenerator = new ToolTipGenerator(this);

      getInterpretationSettings().getNavigationHistory().addZSettings(zSettings);

      zSettings.minZ.setPersistable(false);
      zSettings.maxZ.setPersistable(false);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getSizeChangeManager(), newCoalescingExecListener(this::resized));

      Listener echogramAreaListener = newCoalescingExecListener(this::echogramAreaChanged);
      registry.add(echogramAreaListener, List.of(
            zSettings.getZoomedChangeManager(),
            getInterpretationSettings().getDataLoadingModeChangeManager(),
            getInterpretationSettings().getPingMappingChangeManager(),
            getInterpretationSettings().getPingRangeChangeManager(),
            getConfigurationManager().getSurveyMiscConf().seabedMounted
      ));

      registry.add(getConfigurationManager().getSurveyConf().mSurvey, this::repaint); // Needed for message about survey not opened. See #108

      if (!isPelagic()) {
         registry.add(getInterpretationSettings().getChannelChangeManager(), echogramAreaListener); // Needed for bottom transform
      }

      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(this::updateToolTipText));

      Listener updateMouseEchogramPointListener = newCoalescingExecListener(this::updateMouseEchogramPoint);
      registry.add(mousePosition(), updateMouseEchogramPointListener);
      registry.add(getInterpretationSettings().mouseover().frozen(), _ -> {
         if (getMousePosition() != null) {
            updateMouseEchogramPointListener.listen();
         }
      });

      registry.add(getInterpretationSettings().getEchogramSettings().workingMode, newCoalescingExecListener(this::updateBackgroundOverlay));

      //---

      resized();
      updateToolTipText();
      updateBackgroundOverlay();
   }

   @Override
   public abstract ViewHolder<? extends BaseEchogramView> getViewHolder();

   @Override
   public HelpID getHelpID() {
      return LsssHelp.ECHOGRAM_MODULE;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      EchogramSettings workingMode = getInterpretationSettings().getEchogramSettings();
      return List.of(
            zSettings.minZ,
            zSettings.maxZ,
            //---
            new HeaderParameter("Working mode"),
            workingMode.workingMode,
            //---
            new HeaderParameter("Edit", MiscIcons.EDIT),
            workingMode.editSubModeDefault,
            workingMode.editSubModeHorizontalLayerBoundary,
            workingMode.editSubModeVerticalLayerBoundary,
            workingMode.editSubModeLayerConnector,
            workingMode.editSubModeSchoolBoundary,
            workingMode.editSubModeSchoolInterior,
            //---
            new HeaderParameter("Delete", MiscIcons.ERASE),
            workingMode.deleteSubMode,
            workingMode.deleteDrawMode,
            workingMode.deleteBoxSize,
            workingMode.deleteVerticalBarWidth,
            workingMode.deleteHorizontalBarHeight,
            //---
            new HeaderParameter("Zoom", MiscIcons.ZOOM),
            workingMode.zoomSubMode,
            //---
            new HeaderParameter("Add", MiscIcons.ADD),
            workingMode.addSubMode
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   private void resized() {
      int width = Math.max(1, getWidth());
      int height = Math.max(1, getHeight());

      getInterpretationSettings().setSampledPingCount(width);
      zSettings.setHeight(height);

      echogramAreaChanged();
   }

   private void echogramAreaChanged() {
      echogramArea.setValue(new EchogramArea(getPingSettings().getPingRange(), zSettings.getZoomedZRange(), getWidth(), getHeight()));
      if (getMousePosition() != null) {
         updateMouseEchogramPoint();
      }

      updateActiveOverlayLater();
      updateToolTipText();
      repaint();
   }

   public EchogramArea getEchogramArea() {
      return echogramArea.getValue();
   }

   public ObservableValue<EchogramArea> echogramArea() {
      return echogramArea;
   }

   private void updateMouseEchogramPoint() {
      if (getInterpretationSettings().getDataFileSet().isEmpty()) {
         return;
      }
      Point mousePosition = getMousePosition();
      EchogramPoint echogramPoint = mousePosition != null ? imagePointToEchogramPoint(mousePosition) : null;
      getInterpretationSettings().mouseover().setPos(echogramPoint);
   }

   private void updateBackgroundOverlay() {
      setBackgroundOverlay(getInterpretationSettings().getEchogramSettings().workingMode.getValue().overlayClass);
   }

   public boolean isPelagic() {
      return zSettings.isPelagic();
   }

   public IntegrationArea getIntegrationArea() {
      return isPelagic() ? IntegrationArea.PELAGIC : IntegrationArea.BOTTOM;
   }

   public ScatterTypeEnum getScatterTypeEnum() {
      return isPelagic() ? ScatterTypeEnum.PELAGIC : ScatterTypeEnum.BOTTOM;
   }

   public ScatterTypeEnum getSchoolScatterTypeEnum() {
      return isPelagic() ? ScatterTypeEnum.PELAGIC_SCHOOL : ScatterTypeEnum.BOTTOM_SCHOOL;
   }

   public EchogramPingSettings getPingSettings() {
      return getInterpretationSettings().getPingSettings();
   }

   public EchogramZSettings getZSettings() {
      return zSettings;
   }

   public void setSelectionRectangle(@Nullable EchogramRectangle selectionRectangle) {
      toolTipGenerator.setSelectionRectangle(selectionRectangle);
      updateToolTipText();
   }

   public @Nullable EchogramPoint imagePointToEchogramPoint(Point2D point) {
      PingIndex pingIndex = getPingSettings().xToContainingPingIndex(point.getX());
      return pingIndex != null ? new EchogramPoint(pingIndex, zSettings.yToDepth(point.getY(), pingIndex)) : null;
   }

   public Point2D echogramPointToImagePoint(EchogramPoint echogramPoint) {
      return new Point2D.Float(getPingSettings().pingIndexToX(echogramPoint.pingIndex()),
            zSettings.depthToY(echogramPoint.depth(), echogramPoint.pingIndex()));
   }

   private void drawInfoText(Graphics2D g2d) {
      Color color = Color.WHITE;
      float x = getWidth() / 2f;
      float y = getHeight() / 2f;
      int size = 18;
      Font previousFont = g2d.getFont();
      g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, size));
      GuiText.draw(g2d, "Survey not opened.", color, x, y, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, null);
      GuiText.draw(g2d, "Use \"File\" menu to open survey or create new.", color, x, y, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.TOP, null);
      g2d.setFont(previousFont);
   }

   @Override
   protected void draw(Graphics2D g2d) {
      if (getInterpretationSettings().getPingRange().isEmpty()) {
         g2d.setColor(Color.BLACK);
         g2d.fillRect(0, 0, getWidth(), getHeight());
         if (getConfigurationManager().getSurveyConf().getSurvey() == null && isPelagic()) {
            drawInfoText(g2d);
         }
      }
   }

   @Override
   protected boolean shouldUseOverlays() {
      return !getInterpretationSettings().getPingRange().isEmpty();
   }

   @Override
   protected @Nullable String getDefaultToolTipText(Point point) {
      return toolTipGenerator.getToolTip(point);
   }

   @Override
   public JComponent createConfigurationEditor() {
      JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.add("Overlays", createOverlayEditor());
      tabbedPane.add("Tooltip", createTooltipEditor());
      tabbedPane.add("Parameters", super.createConfigurationEditor());
      tabbedPane.setSelectedIndex(editorTabIndex);
      tabbedPane.addChangeListener(_ -> editorTabIndex = tabbedPane.getSelectedIndex());
      return tabbedPane;
   }

   private JComponent createTooltipEditor() {
      ParameterEditor toolTipEditor = new ParameterEditor(toolTipGenerator.getParameters());

      JButton allOnButton = new JButton("All on");
      allOnButton.addActionListener(_ -> {
         Utils.getAllOfType(toolTipGenerator.getParameters(), BooleanParameter.class).forEach(BooleanParameter::setTrue);
      });

      JButton allOffButton = new JButton("All off");
      allOffButton.addActionListener(_ -> {
         Utils.getAllOfType(toolTipGenerator.getParameters(), BooleanParameter.class).forEach(BooleanParameter::setFalse);
      });

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      buttonPanel.add(allOnButton);
      buttonPanel.add(allOffButton);

      Box mainBox = Box.createVerticalBox();
      mainBox.add(toolTipEditor.getEditorComponent());
      mainBox.add(buttonPanel);

      return GuiUtils.createScrollPane(mainBox);
   }

   public abstract static class BaseEchogramView extends BaseOverlaidView {
      private final EchogramModule module;
      private final LSSS lsss;
      private final JPanel panel = new JPanel(new BorderLayout());

      BaseEchogramView(EchogramModule module) {
         super(module);

         this.module = module;
         lsss = module.getLSSS();

         JComponent component = super.getComponent();
         component.setTransferHandler(lsss.getConfigurationManager().getDataConf().getDataFileTransferHandler());
         component.addMouseWheelListener(new EchogramZoomMouseWheelListener(module.getPingSettings(), module.getZSettings(), lsss.getInterpretationSettings().getNavigationHistory()));
         component.setMinimumSize(new Dimension(0, 10));

         panel.add(component);
         panel.add(module.verticalEchogramScrollBar.getComponent(), BorderLayout.EAST);
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }

      @Override
      public JComponent getApiComponent() {
         return super.getComponent();
      }

      @Override
      protected KeyListener getKeyListener() {
         return new EchogramKeyListener();
      }

      @Override
      protected JPopupMenu getDefaultPopupMenu(Point point) {
         EchogramPoint echogramPoint = module.imagePointToEchogramPoint(point);
         Ping ping;
         if (echogramPoint != null) {
            ping = module.getInterpretationSettings().getDataFileSet().getPing(echogramPoint.pingIndex());
         } else {
            ping = null;
         }
         RangeSet<PingIndex> selectedPingRanges = new ArrayRangeSet<>();
         lsss.getRegionManager().getSelectedRegions().stream()
               .map(Region::getPingRange)
               .map(lsss.getInterpretationSettings().getPingRange()::intersection)
               .forEach(selectedPingRanges::add);

         JPopupMenu mainPopupMenu = new JPopupMenu();

         boolean pelagicMode = lsss.getConfigurationManager().getSurveyMiscConf().pelagicMode.getBooleanValue();
         JMenuItem pelagicModeItem = MiscIcons.checkBox(pelagicMode).on(mainPopupMenu.add("Pelagic mode"));
         pelagicModeItem.addActionListener(_ -> lsss.getConfigurationManager().getSurveyMiscConf().pelagicMode.toggle());

         mainPopupMenu.addSeparator();

         mainPopupMenu.add(module.createConfigureMenuItem());

         addConfigureOverlaysItems(point, mainPopupMenu);

         addDisableOverlaysItems(point, mainPopupMenu);

         mainPopupMenu.addSeparator();

         JMenu pingMappingPopupMenu = new JMenu("Ping mapping");
         mainPopupMenu.add(pingMappingPopupMenu);
         for (PingMapping pingMapping : PingMapping.values()) {
            boolean selected = pingMapping == lsss.getInterpretationSettings().getPingMapping();
            JMenuItem pingMappingItem = MiscIcons.check(selected).on(pingMappingPopupMenu.add(pingMapping.toString()));
            pingMappingItem.addActionListener(_ -> lsss.getInterpretationSettings().setPingMapping(pingMapping));
         }

         JMenu channelPopupMenu = new JMenu("Frequency");
         mainPopupMenu.add(channelPopupMenu);
         if (lsss.getInterpretationSettings().getDataFileSet().isEmpty()) {
            channelPopupMenu.setEnabled(false);
         } else {
            RawFileConfiguration rawFileConfiguration = lsss.getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
            for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
               int kHz = rawFileConfiguration.getTransducers().get(channel - 1).getKHz();
               boolean selected = channel == lsss.getInterpretationSettings().getChannel();
               JMenuItem channelItem = MiscIcons.check(selected).on(channelPopupMenu.add(kHz + " kHz"));
               int finalChannel = channel;
               channelItem.addActionListener(_ -> lsss.getInterpretationSettings().setChannel(finalChannel));
            }
         }

         JMenu modePopupMenu = new JMenu("Data loading mode");
         mainPopupMenu.add(modePopupMenu);
         for (DataLoadingMode mode : DataLoadingMode.values()) {
            boolean selected = mode == lsss.getInterpretationSettings().getDataLoadingMode();
            JMenuItem modeItem = MiscIcons.check(selected).on(modePopupMenu.add(mode.toString()));
            modeItem.addActionListener(_ -> lsss.getInterpretationSettings().setDataLoadingMode(mode));
         }

         addToPopupMenu(mainPopupMenu);

         mainPopupMenu.addSeparator();

         EchogramModuleUtils.addZoomItems(mainPopupMenu, lsss, module.getZSettings());

         mainPopupMenu.addSeparator();

         CommentDataModule commentDataModule = lsss.getModuleManager().getModule(CommentDataModule.class);

         mainPopupMenu.add(commentDataModule.popupMenuItem(echogramPoint != null ? echogramPoint.pingIndex() : null));

         JMenuItem tag0ToCommentItem = mainPopupMenu.add("Add comments from TAG0 datagrams...");
         if (echogramPoint == null) {
            tag0ToCommentItem.setEnabled(false);
         } else {
            tag0ToCommentItem.addActionListener(_ -> commentDataModule.createCommentsFromTag0Datagrams());
         }

         mainPopupMenu.addSeparator();

         JMenuItem mergeLayersItem = mainPopupMenu.add("Merge selected layers");
         mergeLayersItem.setEnabled(lsss.getRegionManager().getLayerManager().getSelectedRegions().size() > 1);
         mergeLayersItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
         mergeLayersItem.addActionListener(_ -> LsssUtils.mergeSelectedLayers(lsss));

         JMenuItem deleteSchoolsItem = MiscIcons.DELETE.on(mainPopupMenu.add("Delete selected schools"));
         deleteSchoolsItem.setEnabled(!lsss.getRegionManager().getSchoolManager().getSelectedRegions().isEmpty());
         deleteSchoolsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
         deleteSchoolsItem.addActionListener(_ -> LsssUtils.deleteSelectedSchools(lsss));

         JMenuItem mergeSchoolsItem = mainPopupMenu.add("Merge selected schools");
         mergeSchoolsItem.setEnabled(lsss.getRegionManager().getSchoolManager().getSelectedRegions().size() > 1);
         mergeSchoolsItem.addActionListener(_ -> LsssUtils.mergeSelectedSchools(lsss));

         RegionEditOverlay regionEditOverlay = module.getBackgroundOverlay(RegionEditOverlay.class);
         if (regionEditOverlay != null) {
            CurveBoundary activeCurveBoundary = regionEditOverlay.getActiveCurveBoundary();
            if (activeCurveBoundary != null && echogramPoint != null) {
               JMenuItem addHorizontalLayerBoundaryItem = mainPopupMenu.add("Add horizontal layer boundary...");
               addHorizontalLayerBoundaryItem.addActionListener(_ -> {
                  new SimpleInputDialog<>("Insert horizontal layer boundary", "Depth offset to existing layer boundary", "", Float::parseFloat)
                        .setUnit(Unit.METER)
                        .setBelowText("A positive offset means deeper.")
                        .show(getComponent())
                        .ifPresent(inputOffset -> {
                           float offset = lsss.getDataManager().getDataConfiguration().isSeabedMounted() ? -inputOffset : inputOffset;
                           ToFloatFunction<PingIndex> pingIndexToDepth = pingIndex -> activeCurveBoundary.getCurve().getClampedDepth(pingIndex) + offset;
                           lsss.getRegionManager().addHorizontalLayerBoundary(echogramPoint.pingIndex(), pingIndexToDepth);
                        });
               });
            }
         }

         mainPopupMenu.addSeparator();

         ExclusionManager exclusionManager = lsss.getRegionManager().getExclusionManager();

         JMenuItem excludeItem = LsssIcons.EXCLUDE.on(mainPopupMenu.add("Exclude selected regions range"));
         if (selectedPingRanges.stream().allMatch(exclusionManager.getExclusions()::containsAll)) {
            excludeItem.setEnabled(false);
         } else {
            excludeItem.addActionListener(_ -> {
               selectedPingRanges.forEach(exclusionManager::excludeRange);
            });
         }

         JMenuItem includeItem = mainPopupMenu.add("Include selected regions range");
         if (selectedPingRanges.stream().allMatch(exclusionManager.getExclusions()::containsNone)) {
            includeItem.setEnabled(false);
         } else {
            includeItem.addActionListener(_ -> {
               selectedPingRanges.forEach(exclusionManager::includeRange);
            });
         }

         mainPopupMenu.addSeparator();

         PreprocessingConf preprocessingConf = lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf();
         EchogramModuleUtils.addViewPreprocessingMenuItem(mainPopupMenu, ping, preprocessingConf);

         JMenuItem processingStartItem = LsssIcons.KORONA.on(mainPopupMenu.add("Start preprocessing on visible files"));
         processingStartItem.setEnabled(echogramPoint != null);
         processingStartItem.addActionListener(_ -> {
            PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
            List<DataFile> dataFiles = lsss.getConfigurationManager().getDataConf().getDataSetManager().getDataFileSet(DataType.RAW).getDataFiles(pingRange);
            SegmentHandle first = dataFiles.getFirst().getSegmentHandle();
            SegmentHandle last = dataFiles.getLast().getSegmentHandle();
            preprocessingConf.getMainSetup().start(Optional.of(new DefaultRange<>(first, last)));
         });

         return mainPopupMenu;
      }

      abstract void addToPopupMenu(JPopupMenu popupMenu);

      private final class EchogramKeyListener extends KeyAdapter {
         private EchogramKeyListener() {
         }

         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_TAB -> EchogramKeys.tab(e, module);
               case KeyEvent.VK_UP -> EchogramKeys.up(e, module.getZSettings());
               case KeyEvent.VK_DOWN -> EchogramKeys.down(e, module.getZSettings());
               case KeyEvent.VK_LEFT -> EchogramKeys.left(e, lsss.getInterpretationSettings());
               case KeyEvent.VK_RIGHT -> EchogramKeys.right(e, lsss.getInterpretationSettings());
               case KeyEvent.VK_HOME -> EchogramKeys.home(e, lsss.getInterpretationSettings(), module.getZSettings());
               case KeyEvent.VK_PAGE_UP -> {
                  if (e.getModifiersEx() == 0) {
                     lsss.getInterpretationSettings().shiftChannel(1);
                  }
               }
               case KeyEvent.VK_PAGE_DOWN -> {
                  if (e.getModifiersEx() == 0) {
                     lsss.getInterpretationSettings().shiftChannel(-1);
                  }
               }
               case KeyEvent.VK_DELETE -> {
                  if (e.getModifiersEx() == 0) {
                     LsssUtils.deleteSelectedSchools(lsss);
                  }
               }
               case KeyEvent.VK_A -> {
                  if (e.getModifiersEx() == 0) {
                     lsss.getRegionManager().selectRegions(lsss.getRegionManager().getVisibleRegions());
                  }
               }
               case KeyEvent.VK_D -> {
                  if (e.getModifiersEx() == 0) {
                     lsss.getInterpretationSettings().shiftChannel(-1);
                  }
               }
               case KeyEvent.VK_F -> {
                  if (e.getModifiersEx() == 0) {
                     lsss.getInterpretationSettings().shiftChannel(1);
                  }
               }
               case KeyEvent.VK_M -> {
                  if (e.getModifiersEx() == 0) {
                     LsssUtils.mergeSelectedLayers(lsss);
                  }
               }
               case KeyEvent.VK_N -> {
                  if (e.getModifiersEx() == 0) {
                     lsss.getRegionManager().setNextRegionSelected();
                  } else if (e.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK) {
                     lsss.getRegionManager().setPreviousRegionSelected();
                  }
               }
               default -> {
               }
            }
         }
      }
   }
}
