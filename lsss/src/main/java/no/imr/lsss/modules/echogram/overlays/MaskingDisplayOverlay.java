package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Mask;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.interpretation.InterpretationSummary;
import no.imr.lsss.modules.interpretation.StoreUtils;
import no.imr.lsss.modules.masking.ConditionalMaskingExtractionModule;
import no.imr.lsss.util.LsssUtils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.IntRangeSet;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import no.imr.tools.swing.GuiUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Displays masking.
 */
public final class MaskingDisplayOverlay extends BaseEchogramOverlay {
   private static final Color CONDITIONAL_MASKING_COLOR = new Color(0xe6e6e6e6, true);
   private static final Color MASKING_COLOR = new Color(0x99000000, true);
   private static final Color EXCLUSION_COLOR = new Color(0x66000000, true);
   public static final Color STORED_COLOR = new Color(0x66008000, true);
   private static final Color CLEAR_COLOR = new Color(0x0, true);

   private static final int SLOPING_LINES_INTERVAL = 20;

   private VolatileImage volatileImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleVolatileImage(1, 1);

   private final Queue<Range<Integer>> rangeQueue = new ConcurrentLinkedQueue<>();
   private final Queue<Updates> updatesQueue = new ConcurrentLinkedQueue<>();

   private final Supplier<ConditionalMaskingExtractionModule> conditionalMaskExtractor = moduleSupplier(ConditionalMaskingExtractionModule.class);

   public MaskingDisplayOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getEchogramModule().getSizeChangeManager(), newCoalescingExecListener(this::resized));

      registry.add(newCoalescingExecListener(this::updateGraphics), List.of(
            getEchogramModule().echogramArea(),
            getInterpretationSettings().getChannelChangeManager(),
            getRegionManager().getStoringConfigManager().getChangeManager(),
            getLSSS().getInterpretationSummary().getChangeManager(),
            getLSSS().getActions().showStoredMasking.getChangeManager(),
            getLSSS().getActions().showConditionalMasking.getChangeManager(),
            conditionalMaskExtractor.get().getChangeManager()
      ));

      Consumer<PingRange> pingRangeListener = newExecListener(pingRange -> {
         int x0 = (int) Math.floor(getPingSettings().pingIndexToX(pingRange.begin()));
         int x1 = (int) Math.ceil(getPingSettings().pingIndexToX(pingRange.end()));
         if (x0 > x1) {
            // Can happen if ping settings is modified between the two calls.
            return;
         }
         updateGraphics(x0, x1 + 1);
      });
      registry.add(pingRangeListener, List.of(
            getRegionManager().getExclusionManager().getChangeManager(),
            getRegionManager().getMaskingManager().getChangeManager()
      ));

      //---

      resized();
   }

   @Override
   protected OverlayDisplayData recomputeDisplayData() {
      return new DisplayData();
   }

   private void resized() {
      createVolatileImage();
      updateGraphics();
      recompute();
   }

   private void createVolatileImage() {
      volatileImage.flush();
      int width = Math.max(1, getWidth());
      int height = Math.max(1, getHeight());
      volatileImage = getOverlaidModule().getGraphicsConfiguration().createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
   }

   private void draw(Graphics2D g2d) {
      do {
         int returnCode = volatileImage.validate(g2d.getDeviceConfiguration());
         if (returnCode == VolatileImage.IMAGE_RESTORED) {
            // Contents need to be restored
            updatesQueue.clear();
            addUpdate(0, getWidth());
         } else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
            // old vImg doesn't work with new GraphicsConfig; re-create it
            createVolatileImage();
            updatesQueue.clear();
            addUpdate(0, getWidth());
         }

         if (!updatesQueue.isEmpty()) {
            Graphics2D imageG2d = volatileImage.createGraphics();
            imageG2d.setBackground(CLEAR_COLOR);

            while (true) {
               Updates updates = updatesQueue.poll();
               if (updates == null) {
                  break;
               }
               updates.apply(imageG2d, volatileImage.getHeight());
            }

            imageG2d.dispose();
         }

         g2d.drawImage(volatileImage, 0, 0, null);
      } while (volatileImage.contentsLost());
   }

   private static void clear(Graphics2D g2d, Range<Integer> xRange, int height) {
      int x0 = xRange.begin();
      int x1 = xRange.end();
      g2d.clearRect(x0, 0, x1 - x0, height);
   }

   private static void drawExclusionUpdates(Graphics2D g2d, RangeSet<Integer> exclusionUpdates, int height) {
      g2d.setColor(EXCLUSION_COLOR);
      fill(g2d, exclusionUpdates, height);

      g2d.setStroke(GuiUtils.STROKE_1);
      g2d.setColor(Color.BLACK);
      drawSlopingLines(g2d, exclusionUpdates, height);
   }

   public static void fill(Graphics2D g2d, RangeSet<Integer> xRanges, int height) {
      xRanges.forEachBeginEnd((x0, x1) -> {
         g2d.fillRect(x0, 0, x1 - x0, height);
      });
   }

   public static void drawSlopingLines(Graphics2D g2d, RangeSet<Integer> xRanges, int height) {
      Shape clip = g2d.getClip();
      xRanges.forEachBeginEnd((x0, x1) -> {
         int width = x1 - x0;
         g2d.setClip(new Rectangle2D.Float(x0, 0, width, height));
         int d = width - 1;
         for (int y = x0 % SLOPING_LINES_INTERVAL - SLOPING_LINES_INTERVAL * (width / SLOPING_LINES_INTERVAL + 1); y < height; y += SLOPING_LINES_INTERVAL) {
            g2d.drawLine(x0, y, x0 + d, y + d);
         }
      });
      g2d.setClip(clip);
   }

   private static void drawUpdates(Graphics2D g2d, Map<Integer, IntRangeSet> updates, Color color) {
      g2d.setColor(color);
      updates.forEach((x, intRangeSet) -> {
         int[] indexes = intRangeSet.getIndexes();
         for (int i = 0; i < indexes.length; ) {
            int beginIndex = indexes[i++];
            int endIndex = indexes[i++];
            g2d.fillRect(x, beginIndex, 1, endIndex - beginIndex);
         }
      });
   }

   private static void drawStoredUpdates(Graphics2D g2d, List<Rectangle2D> storeUpdates) {
      g2d.setColor(STORED_COLOR);
      storeUpdates.forEach(g2d::fill);
   }

   private static void drawStoringConfigUpdates(Graphics2D g2d, RangeSet<Integer> xRanges, int height) {
      g2d.setStroke(GuiUtils.STROKE_3);
      g2d.setColor(STORED_COLOR);
      drawSlopingLines(g2d, xRanges, height);
   }

   private void updateGraphics() {
      updateGraphics(0, getWidth());
   }

   private void updateGraphics(int beginX, int endX) {
      rangeQueue.add(new DefaultRange<>(beginX, endX));
      executeIfEnabled(() -> {
         RangeSet<Integer> rangeSet = new ArrayRangeSet<>();
         while (true) {
            Range<Integer> range = rangeQueue.poll();
            if (range == null) {
               break;
            }
            rangeSet.add(range);
         }
         updatesQueue.removeIf(updates -> rangeSet.containsAll(updates.xRange));
         rangeSet.forEachBeginEnd(this::addUpdate);
         repaint();
      });
   }

   private void addUpdate(int beginX, int endX) {
      if (getInterpretationSettings().getPingRange().isEmpty()) {
         return;
      }
      beginX = Math.clamp(beginX, 0, getWidth());
      endX = Math.clamp(endX, 0, getWidth());
      updatesQueue.add(new Updates(this, beginX, endX));
   }

   private List<Rectangle2D> createStoredUpdates(int beginX, int endX) {
      if (!getLSSS().getActions().showStoredMasking.get()) {
         return List.of();
      }

      EchogramPingSettings pingSettings = getPingSettings();
      EchogramZSettings zSettings = getZSettings();

      List<Rectangle2D> storeUpdates = new ArrayList<>();

      InterpretationSummary interpretationSummary = getLSSS().getInterpretationSummary();

      Collection<Scatter> scatters = interpretationSummary.getScatterSet().getScatters(getEchogramModule().getScatterTypeEnum(), getInterpretationSettings().getFrequency());
      for (Scatter scatter : scatters) {
         PingRange scatterPingRange = LsssUtils.getPingRange(getInterpretationSettings().getDataFileSet(), scatter);
         if (!scatterPingRange.intersects(getInterpretationSettings().getPingRange())) {
            continue;
         }

         int x0 = pingSettings.pingIndexToXIndex(scatterPingRange.begin());
         int x1 = pingSettings.pingIndexToXIndex(scatterPingRange.end());

         x0 = Math.max(x0, beginX);
         x1 = Math.min(x1, endX);
         if (x0 >= x1) {
            continue;
         }

         FloatRange zRange = StoreUtils.getStoredZRange(scatter);
         float minZ = zRange.min();
         float maxZ = zRange.max();

         float y0 = zSettings.zToYIndex(minZ);
         float y1 = zSettings.zToYIndex(maxZ);

         storeUpdates.add(new Rectangle2D.Float(x0, y0, x1 - x0, y1 - y0));
      }
      return storeUpdates;
   }

   private RangeSet<Integer> createStoringConfigUpdates(int beginX, int endX) {
      RangeSet<Integer> storeConfigUpdates = new ArrayRangeSet<>();

      if (!getLSSS().getActions().showStoredMasking.get()) {
         return storeConfigUpdates;
      }

      EchogramPingSettings pingSettings = getPingSettings();

      getRegionManager().getStoringConfigManager().getConfigMap().forEach(entry -> {
         if (entry.value().pelagicMode() && !getEchogramModule().isPelagic()) {
            return;
         }
         Range<PingIndex> pingRange = entry.range();
         int x0 = Math.max(beginX, pingSettings.pingIndexToXIndex(pingRange.begin()));
         int x1 = Math.min(endX, pingSettings.pingIndexToXIndex(pingRange.end()));
         if (x0 >= x1) {
            return;
         }
         storeConfigUpdates.add(x0, x1);
      });

      getLSSS().getInterpretationSummary().getStoredPings().forEach(pingRange -> {
         int x0 = pingSettings.pingIndexToXIndex(pingRange.begin());
         int x1 = pingSettings.pingIndexToXIndex(pingRange.end());
         storeConfigUpdates.remove(x0, x1);
      });

      getRegionManager().getExclusionManager().getExclusions().forEach(pingRange -> {
         int x0 = pingSettings.pingIndexToXIndex(pingRange.begin());
         int x1 = pingSettings.pingIndexToXIndex(pingRange.end());
         storeConfigUpdates.remove(x0, x1);
      });

      return storeConfigUpdates;
   }

   private RangeSet<Integer> createExclusionUpdates(int beginX, int endX) {
      EchogramPingSettings pingSettings = getPingSettings();
      RangeSet<Integer> updates = new ArrayRangeSet<>();
      getRegionManager().getExclusionManager().getExclusions().stream().forEach(pingRange -> {
         int x0 = Math.max(beginX, pingSettings.pingIndexToXIndex(pingRange.begin()));
         int x1 = Math.min(endX, pingSettings.pingIndexToXIndex(pingRange.end()));
         if (x0 >= x1) {
            return;
         }
         updates.add(x0, x1);
      });
      return updates;
   }

   private Map<Integer, IntRangeSet> createMaskUpdates(int beginX, int endX, RangeSet<Integer> exclusionUpdates) {
      EchogramPingSettings pingSettings = getPingSettings();
      EchogramZSettings zSettings = getZSettings();
      Map<Integer, IntRangeSet> updates = HashMap.newHashMap(endX - beginX);
      Mask mask = getRegionManager().getMaskingManager().getMask(getInterpretationSettings().getChannel());
      for (int x = beginX; x < endX; x++) {
         if (exclusionUpdates.contains(x)) {
            continue;
         }
         PingIndex pingIndex = pingSettings.xToContainingOrClosestPingIndex(x + 0.5);
         addDepthRangeUpdates(updates, x, pingIndex, mask.get(pingIndex), zSettings);
      }
      return updates;
   }

   private Map<Integer, IntRangeSet> createConditionalUpdates(int beginX, int endX) {
      if (!getLSSS().getActions().showConditionalMasking.get()) {
         return Map.of();
      }
      EchogramPingSettings pingSettings = getPingSettings();
      EchogramZSettings zSettings = getZSettings();
      Map<Integer, IntRangeSet> updates = HashMap.newHashMap(endX - beginX);
      for (int x = beginX; x < endX; x++) {
         PingIndex pingIndex = pingSettings.xToContainingOrClosestPingIndex(x + 0.5);
         FloatRangeSet depthRanges = conditionalMaskExtractor.get().pingIndexToClosestDepthRanges(pingIndex);
         addDepthRangeUpdates(updates, x, pingIndex, depthRanges, zSettings);
      }
      return updates;
   }

   private static void addDepthRangeUpdates(Map<Integer, IntRangeSet> updates, int x, PingIndex pingIndex,
                                            FloatRangeSet depthRanges, EchogramZSettings zSettings) {
      if (depthRanges.isEmpty()) {
         return;
      }
      int h = zSettings.getHeight();
      IntRangeSet intRangeSet = new IntRangeSet();
      for (FloatRange depthRange : depthRanges) {
         float y0 = Math.clamp(zSettings.depthToY(depthRange.min(), pingIndex), 0, h);
         float y1 = Math.clamp(zSettings.depthToY(depthRange.max(), pingIndex), 0, h);
         if (y0 <= y1) {
            intRangeSet.add((int) Math.floor(y0), (int) Math.ceil(y1));
         } else {
            intRangeSet.add((int) Math.floor(y1), (int) Math.ceil(y0));
         }
      }
      updates.put(x, intRangeSet);
   }

   private static final class Updates {
      private final Range<Integer> xRange;
      private final RangeSet<Integer> exclusionUpdates;
      private final Map<Integer, IntRangeSet> maskUpdates;
      private final List<Rectangle2D> storeUpdates;
      private final RangeSet<Integer> storingConfigUpdates;
      private final Map<Integer, IntRangeSet> conditionalUpdates;

      private Updates(MaskingDisplayOverlay overlay, int beginX, int endX) {
         xRange = new DefaultRange<>(beginX, endX);
         storeUpdates = overlay.createStoredUpdates(beginX, endX);
         storingConfigUpdates = overlay.createStoringConfigUpdates(beginX, endX);
         exclusionUpdates = overlay.createExclusionUpdates(beginX, endX);
         maskUpdates = overlay.createMaskUpdates(beginX, endX, exclusionUpdates);
         conditionalUpdates = overlay.createConditionalUpdates(beginX, endX);
      }

      private void apply(Graphics2D g2d, int height) {
         clear(g2d, xRange, height);
         drawExclusionUpdates(g2d, exclusionUpdates, height);
         drawUpdates(g2d, maskUpdates, MASKING_COLOR);
         drawUpdates(g2d, conditionalUpdates, CONDITIONAL_MASKING_COLOR);
         drawStoredUpdates(g2d, storeUpdates);
         drawStoringConfigUpdates(g2d, storingConfigUpdates, height);
      }
   }

   private final class DisplayData extends OverlayDisplayData {
      private DisplayData() {
      }

      @Override
      public void draw(Graphics2D g2d) {
         MaskingDisplayOverlay.this.draw(g2d);
      }
   }
}
