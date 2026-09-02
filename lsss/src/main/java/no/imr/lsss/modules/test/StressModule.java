package no.imr.lsss.modules.test;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.region.CurveBoundary;
import no.imr.korona.region.EchogramSelection;
import no.imr.korona.region.IllegalEditException;
import no.imr.korona.region.LayerConnector;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.RegionValidation;
import no.imr.korona.region.School;
import no.imr.korona.region.VerticalBoundary;
import no.imr.korona.region.schooledit.SchoolEditor;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.framework.InterpretationZSettings;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.framework.config.survey.data.DataType;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.interpretation.InterpretationModule;
import no.imr.tools.RandomUtils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.SelectionAction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.swing.VerticalScrollablePanel;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * For testing.
 */
public final class StressModule extends BaseViewModule {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private int stressCounter;
   private final Supplier<InterpretationModule> interpretationModule = moduleSupplier(InterpretationModule.class);
   private final List<StressActionCollection> stressActionCollections = new CopyOnWriteArrayList<>();
   private final List<StressAction> activeStressActions = new ArrayList<>();
   private final Random random = new Random();
   private volatile @Nullable AsyncAccess asyncAccess;

   public StressModule(ModuleInfo<TestPlugin> moduleInfo) {
      super(moduleInfo);

      setSeed(RandomUtils.newSeed());

      add("", List.of(
            List.of(
                  new StressAction("Zoom", this::zoom),
                  new StressAction("Echogram select", this::echogramSelect),
                  new StressAction("Mouse echogram point", this::mouseEchogramPoint),
                  new StressAction("Change channel", this::changeChannel),
                  new StressAction("Change files", this::changeFiles),
                  new StressAction("Toggle raw/processed", this::toggleRawProcessed)
            ),
            List.of(
                  new StressAction("Merge", this::merge),
                  new StressAction("Add curve", this::addCurve),
                  new StressAction("Add vertical", this::addVertical),
                  new StressAction("Add divider", this::addDivider)
            ),
            List.of(
                  new StressAction("Edit vertical", this::editVertical),
                  new StressAction("Edit curve", this::editCurve),
                  new StressAction("Edit connector", this::editConnector)
            ),
            List.of(
                  new StressAction("Add school", this::addSchool),
                  new StressAction("Delete school", this::deleteSchool),
                  new StressAction("Edit school", this::editSchool),
                  new StressAction("Split school", this::splitSchool),
                  new StressAction("Merge schools", this::mergeSchools),
                  new StressAction("Move school", this::moveSchool),
                  new StressAction("Scale school", this::scaleSchool),
                  new StressAction("Undo", this::undo)
            ),
            List.of(
                  new StressAction("Interpret", this::interpret),
                  new StressAction("Bubble correct", this::bubbleCorrect),
                  new StressAction("Store", this::store),
                  new StressAction("Delete", this::delete)
            ),
            List.of(
                  new StressAction("Exclude", this::exclude),
                  new StressAction("Mask", this::mask)
            )
      ));
   }

   public void setRunning(boolean running) {
      viewHolder.ifView(view -> view.setRunning(running));
   }

   public void add(String prefix, List<List<StressAction>> stressActionsList) {
      SwingUtilities.invokeLater(() -> {
         for (List<StressAction> stressActions : stressActionsList) {
            StressActionCollection stressActionCollection = new StressActionCollection(prefix, stressActions);
            stressActionCollections.add(stressActionCollection);
            if (viewHolder.hasView()) {
               viewHolder.getView().add(stressActionCollection);
            }
         }
      });
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      viewHolder.ifView(view -> {
         if (view.running) {
            view.timer.start();
         }
         if (view.asyncAccessCheckBox.isSelected()) {
            asyncAccess = new AsyncAccess(this);
         }
      });
   }

   @Override
   protected void onDisable() {
      viewHolder.ifView(view -> view.timer.stop());
      asyncAccess = null;
   }

   @Override
   public void close() {
      onDisable();
   }

   public void setSeed(long seed) {
      Log.global.config("Random seed = " + seed);
      random.setSeed(seed);
   }

   public void reset() {
      DataConf dataConf = getConfigurationManager().getDataConf();
      dataConf.selectAll();
      dataConf.apply();

      getRegionManager().setupDefaultBoundaries();

      stressCounter = 0;
   }

   public List<JCheckBox> getStressCheckBoxes() {
      return viewHolder.getView().stressCheckBoxes;
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   public Random getRandom() {
      return random;
   }

   private InterpretationZSettings getRandomZSettings() {
      return random.nextBoolean() ? getInterpretationSettings().getPelagicZSettings() : getInterpretationSettings().getBottomZSettings();
   }

   private DepthTransform getRandomDepthTransform() {
      return getRandomZSettings().getDepthTransform();
   }

   private EchogramPoint getRandomEchogramPoint() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      long pingNumber = random.nextLong(pingRange.begin().getPingNumber(), pingRange.end().getPingNumber());
      PingIndex pingIndex = getInterpretationSettings().getDataFileSet().getPingIndex(pingNumber);

      InterpretationZSettings zSettings = getRandomZSettings();
      FloatRange zoomedZRange = zSettings.getZoomedZRange();
      float z = random.nextFloat(zoomedZRange.min(), zoomedZRange.max());
      float depth = zSettings.zToDepth(z, pingIndex);

      return new EchogramPoint(pingIndex, depth);
   }

   private PingRange getRandomPingRange() {
      PingRange totalPingRange = getInterpretationSettings().getDataFileSet().getTotalRange();

      long firstPingNumber = random.nextLong(totalPingRange.begin().getPingNumber(), totalPingRange.end().getPingNumber());
      long lastPingNumber = random.nextLong(firstPingNumber, totalPingRange.end().getPingNumber());

      return PingRange.of(
            getInterpretationSettings().getDataFileSet().getPingIndex(firstPingNumber),
            getInterpretationSettings().getDataFileSet().getPingIndex(lastPingNumber));
   }

   private @Nullable School getRandomSchool() {
      List<School> visibleSchools = getRegionManager().visibleSchools().toList();
      if (visibleSchools.isEmpty()) {
         return null;
      }
      return RandomUtils.get(random, visibleSchools);
   }

   private @Nullable VerticalBoundary getRandomVerticalBoundary() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      Set<VerticalBoundary> verticalBoundaries = getRegionManager().getLayerManager().getLayers().stream()
            .flatMap(layer -> layer.getVerticalBoundaries().stream())
            .filter(verticalBoundary -> pingRange.containsIncludingEnd(verticalBoundary.getPingIndex()))
            .collect(Collectors.toSet());
      if (verticalBoundaries.isEmpty()) {
         return null;
      }
      return RandomUtils.get(random, verticalBoundaries);
   }

   private @Nullable CurveBoundary getRandomCurveBoundary() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      Set<CurveBoundary> curveBoundaries = getRegionManager().getLayerManager().getLayers().stream()
            .flatMap(layer -> Stream.concat(layer.getUpperCurveBoundaries().stream(), layer.getLowerCurveBoundaries().stream()))
            .filter(curveBoundary -> curveBoundary.getPingRange().intersects(pingRange))
            .collect(Collectors.toSet());
      if (curveBoundaries.isEmpty()) {
         return null;
      }
      return RandomUtils.get(random, curveBoundaries);
   }

   public static void changeFiles(DataConf dataConf, Random random) {
      if (random.nextFloat() < 0.8) {
         // Not too often...
         return;
      }

      List<SegmentHandle> segmentHandles = dataConf.createSegmentHandles(dataConf.getRawDir().getFile()).segmentHandles();
      dataConf.selectSegmentHandles(RandomUtils.getSubList(random, segmentHandles));
      dataConf.apply();
   }

   public void doOneStressAction() {
      if (getInterpretationSettings().getDataFileSet().isEmpty()) {
         viewHolder.getView().updateLabel("No data");
         return;
      }
      if (activeStressActions.isEmpty()) {
         viewHolder.getView().updateLabel("No actions");
         return;
      }
      StressAction stressAction = RandomUtils.get(random, activeStressActions);
      stressCounter++;

      /*
      String text = stressCounter + "   stressAction = " + stressAction;
      System.err.println(text);
      if (stressCounter == 48) {
         JDialog dialog = new JDialog(null, "Debug", Dialog.ModalityType.DOCUMENT_MODAL);
         dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
         dialog.getContentPane().add(new JLabel(text));
         dialog.pack();
         dialog.setVisible(true);
      }
      */

      viewHolder.getView().updateLabel(stressAction.toString());

      try {
         stressAction.action().run();
         RegionValidation.checkLayers(getRegionManager().getLayerManager());
         RegionValidation.checkSchools(getRegionManager().getSchoolManager());
      } catch (IllegalEditException _) {
         // Do not fail because of read-only interpretation.
      } catch (Throwable e) {
         Log.global.log(Level.WARNING, "Stress error, counter: " + stressCounter + ", name: " + stressAction.name(), e);
      }
   }

   private void addVertical() {
      getRegionManager().getLayerManager().addVerticalBoundary(getRandomEchogramPoint());
   }

   private void addCurve() {
      getRegionManager().getLayerManager().addCurveBoundary(getRandomEchogramPoint(), getRandomDepthTransform());
   }

   private void addDivider() {
      getRegionManager().addVerticalDivider(getRandomEchogramPoint().pingIndex());
   }

   private void editVertical() {
      VerticalBoundary verticalBoundary = getRandomVerticalBoundary();
      if (verticalBoundary == null) {
         return;
      }
      getRegionManager().getLayerManager().editVerticalBoundary(getRandomEchogramPoint(), getRandomDepthTransform(), verticalBoundary);
      getRegionManager().getLayerManager().verifyAndAdjustConnectors(verticalBoundary.getLayers());
   }

   private void editCurve() {
      CurveBoundary curveBoundary = getRandomCurveBoundary();
      if (curveBoundary == null) {
         return;
      }
      getRegionManager().getLayerManager().editBoundary(getRandomEchogramPoint(), getRandomEchogramPoint(), getRandomDepthTransform(), curveBoundary);
      getRegionManager().getLayerManager().verifyAndAdjustConnectors(curveBoundary.getLayers());
   }

   private void editConnector() {
      VerticalBoundary verticalBoundary = getRandomVerticalBoundary();
      if (verticalBoundary == null) {
         return;
      }
      LayerConnector connector = random.nextBoolean() ? verticalBoundary.getStartConnector() : verticalBoundary.getEndConnector();
      EchogramPoint toEchogramPoint = getRandomEchogramPoint();
      getRegionManager().getLayerManager().editConnector(toEchogramPoint, getRandomDepthTransform(), connector);
      getRegionManager().getLayerManager().verifyAndAdjustConnectors(connector.getLayers());
   }

   private void addSchool() {
      getRegionManager().addSchool(getRandomEchogramPoint(), getRandomEchogramPoint(), getRandomDepthTransform());
   }

   private void deleteSchool() {
      School school = getRandomSchool();
      if (school == null) {
         return;
      }
      getRegionManager().deleteSchool(school);
   }

   private void editSchool() {
      School school = getRandomSchool();
      if (school == null) {
         return;
      }
      SchoolEditor editor = school.boundaryDrawStart(getRandomEchogramPoint(), getInterpretationSettings().getPingSettings(), getRandomZSettings());
      editor.edit(getRandomEchogramPoint(), getRandomEchogramPoint());
      editor.confirm();
   }

   private void splitSchool() {
      School school = getRandomSchool();
      if (school == null) {
         return;
      }
      SchoolEditor editor = school.boundarySplitStart(getRandomEchogramPoint(), getInterpretationSettings().getPingSettings(), getRandomZSettings());
      editor.edit(getRandomEchogramPoint(), getRandomEchogramPoint());
      editor.confirm();
   }

   private void mergeSchools() {
      School a = getRandomSchool();
      School b = getRandomSchool();
      if (a == null || b == null) {
         return;
      }
      getRegionManager().selectRegions(List.of(a, b));
      getRegionManager().mergeSelectedSchools();
   }

   private void moveSchool() {
      School school = getRandomSchool();
      if (school == null) {
         return;
      }
      SchoolEditor editor = school.moveStart(getRandomEchogramPoint(), getInterpretationSettings().getPingSettings(), getRandomZSettings());
      editor.edit(getRandomEchogramPoint(), getRandomEchogramPoint());
      editor.confirm();
   }

   private void scaleSchool() {
      School school = getRandomSchool();
      if (school == null) {
         return;
      }
      SchoolEditor editor = school.scaleStart(getRandomEchogramPoint(), getInterpretationSettings().getPingSettings(), getRandomZSettings());
      editor.edit(getRandomEchogramPoint(), getRandomEchogramPoint());
      editor.confirm();
   }

   private void undo() {
      if (random.nextBoolean()) {
         getRegionManager().undo();
      } else {
         getRegionManager().redo();
      }
   }

   private void merge() {
      EchogramRectangle echogramRectangle = new EchogramRectangle(getRandomEchogramPoint(), getRandomEchogramPoint(), getRandomDepthTransform());
      getRegionManager().doEchogramSelection(new EchogramSelection(SelectionAction.TOGGLE, echogramRectangle));
      getRegionManager().getLayerManager().mergeSelectedLayers();
   }

   private void zoom() {
      getInterpretationSettings().setPingRange(getRandomPingRange());
   }

   private void echogramSelect() {
      SelectionAction selectionAction = RandomUtils.get(random, SelectionAction.values());
      EchogramRectangle echogramRectangle = new EchogramRectangle(getRandomEchogramPoint(), getRandomEchogramPoint(), getRandomDepthTransform());
      getRegionManager().doEchogramSelection(new EchogramSelection(selectionAction, echogramRectangle));
   }

   private void mouseEchogramPoint() {
      getInterpretationSettings().mouseover().setPos(random.nextBoolean() ? getRandomEchogramPoint() : null);
   }

   private void changeChannel() {
      int channel = 1 + random.nextInt(getInterpretationSettings().getDataFileSet().getTransducerCount());
      getInterpretationSettings().setChannel(channel);
   }

   private void changeFiles() {
      changeFiles(getConfigurationManager().getDataConf(), random);
   }

   private void toggleRawProcessed() {
      DataType dataType = RandomUtils.get(random, DataType.values());
      getConfigurationManager().getDataConf().setSelectDataType(dataType);
   }

   private void interpret() {
      Region region = RandomUtils.get(random, getRegionManager().regionStream().toList());
      AcousticCategory acousticCategory = RandomUtils.get(random, getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories());
      int channel = 1 + random.nextInt(getInterpretationSettings().getDataFileSet().getTransducerCount());
      float assignment = random.nextFloat();
      region.getChannelInterpretation(channel).setAssignment(acousticCategory.getCompId().getAcousticCategory(), assignment);
   }

   private void bubbleCorrect() {
      getRegionManager().getBubbleCorrectionManager().setBubbleCorrection(getRandomPingRange(), random.nextFloat(2));
   }

   private void store() {
      if (!interpretationModule.get().store(InterpretationModule.StoreAction.AUTO)) {
         setRunning(false);
      }
      DatabaseConsistencyChecker.assertConsistency(getLSSS().getDatabaseManager().getDatabaseConnection());
   }

   private void delete() {
      interpretationModule.get().delete(InterpretationModule.DeleteAction.AUTO);
      DatabaseConsistencyChecker.assertConsistency(getLSSS().getDatabaseManager().getDatabaseConnection());
   }

   private void exclude() {
      PingRange pingRange = getRandomPingRange();
      if (random.nextBoolean()) {
         getRegionManager().getExclusionManager().excludeRange(pingRange);
      } else {
         getRegionManager().getExclusionManager().includeRange(pingRange);
      }
   }

   private void mask() {
      PingRange pingRange = getRandomPingRange();
      Map<PingIndex, FloatRangeSet> maskingMap = HashMap.newHashMap(pingRange.getPingCount());
      InterpretationZSettings zSettings = getRandomZSettings();
      getInterpretationSettings().getDataFileSet().getPingIndices(pingRange).forEach(pingIndex -> {
         FloatRange depthRange = zSettings.getDepthRange(pingIndex);
         maskingMap.put(pingIndex, FloatRangeSet.of(RandomUtils.getSubRange(random, depthRange)));
      });

      if (random.nextBoolean()) {
         getRegionManager().getMaskingManager().mask(maskingMap, getInterpretationSettings().getChannel());
      } else {
         getRegionManager().getMaskingManager().unmask(maskingMap, getInterpretationSettings().getChannel());
      }
   }

   private static class AsyncAccess {
      private AsyncAccess(StressModule module) {
         module.asyncAccess = this;
         Exec.CACHED_THREAD_POOL.submit(() -> run(module));
      }

      private void run(StressModule module) {
         int i = 0;
         Duration printInterval = Duration.ofSeconds(1);
         Instant nextPrintTime = Instant.now().plus(printInterval);
         while (module.asyncAccess == this) {
            float x = 0;
            RegionManager regionManager = module.getRegionManager();
            InterpretationSettings interpretationSettings = module.getInterpretationSettings();
            for (Region region : regionManager.getSelectedRegions()) {
               for (PingIndex pingIndex : interpretationSettings.getDataFileSet().getPingIndices(region.getPingRange())) {
                  Ping ping = interpretationSettings.getDataFileSet().getPing(pingIndex);
                  for (FloatRange depthRange : regionManager.getDepthRangesForChannel(region, ping, interpretationSettings.getChannel())) {
                     x += depthRange.getSize();
                  }
               }
            }

            if (Instant.now().isAfter(nextPrintTime)) {
               nextPrintTime = nextPrintTime.plus(printInterval);
               Log.global.info(i++ + ", x = " + x);
            }
         }
      }
   }

   private record StressActionCollection(String prefix, List<StressAction> stressActions) {
   }

   private static final class View extends BaseView {
      private final StressModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final JPanel checkBoxPanel = new VerticalScrollablePanel(new BorderLayout());
      private final JLabel statusLabel = new JLabel("");
      private final Timer timer;
      private final JCheckBox asyncAccessCheckBox = new JCheckBox("Async access");
      private final List<JCheckBox> stressCheckBoxes = new ArrayList<>();
      private final JButton runButton = MiscIcons.PLAY.on(new JButton());
      private boolean running;

      private View(StressModule module) {
         super(module);

         this.module = module;
         timer = new Timer(1, _ -> module.doOneStressAction());
         checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

         runButton.addActionListener(_ -> setRunning(!running));
         runButton.setMargin(new Insets(0, 0, 0, 0));

         Box topPanel = Box.createHorizontalBox();
         topPanel.add(runButton);
         topPanel.add(Box.createHorizontalStrut(5));
         topPanel.add(statusLabel);

         checkBoxPanel.add(asyncAccessCheckBox);
         asyncAccessCheckBox.addActionListener(_ -> {
            module.asyncAccess = asyncAccessCheckBox.isSelected() ? new AsyncAccess(module) : null;
         });

         updateLabel("");

         mainPanel.add(topPanel, BorderLayout.NORTH);
         mainPanel.add(new JScrollPane(checkBoxPanel));

         module.stressActionCollections.forEach(this::add);
      }

      @Override
      public JComponent getComponent() {
         return mainPanel;
      }

      private void setRunning(boolean running) {
         this.running = running;
         module.getInterpretationSettings().setInteractiveMode(!running);
         if (running) {
            MiscIcons.PAUSE.on(runButton);
            timer.start();
         } else {
            MiscIcons.PLAY.on(runButton);
            timer.stop();
         }
         updateLabel("");
      }

      private void add(StressActionCollection stressActionCollection) {
         checkBoxPanel.add(new JSeparator());
         for (StressAction stressAction : stressActionCollection.stressActions) {
            JCheckBox checkBox = new JCheckBox(stressActionCollection.prefix + stressAction);
            stressCheckBoxes.add(checkBox);
            checkBoxPanel.add(checkBox);
            checkBox.addItemListener(_ -> {
               if (checkBox.isSelected()) {
                  module.activeStressActions.add(stressAction);
               } else {
                  module.activeStressActions.remove(stressAction);
               }
               module.activeStressActions.sort(null);
               updateLabel("");
            });
         }

         mainPanel.validate();
         mainPanel.repaint();
      }

      private void updateLabel(String extraText) {
         statusLabel.setText(module.stressCounter + "   ( " + module.activeStressActions.size() + " )   " + extraText);
      }
   }
}
