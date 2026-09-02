package no.imr.lsss.incubator.modules.graphicalinfo;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.incubator.LsssIncubatorFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.lsss.modules.korona.DataFilesCache;
import no.imr.lsss.modules.korona.DataObjectLoader;
import no.imr.tools.Utils;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import org.jspecify.annotations.Nullable;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class GraphicalInfoEchogramOverlay extends BaseEchogramOverlay {
   private final DataFilesCache<GraphicalInfo> graphicalInfoCache = new DataFilesCache<>();
   private DataObjectLoader<GraphicalInfo> graphicalInfoLoader;
   private final ArgChangeManager<GraphicalInfo> graphicalInfoChangeManager = new ArgChangeManager<>();
   private final Map<GraphicalInfo, List<RenderedInfo>> graphicalInfoPaths = new ConcurrentHashMap<>();
   private final List<GraphicalInfo> graphicalInfos = new ArrayList<>();
   private @Nullable RenderedInfo activeRenderedInfo;

   public GraphicalInfoEchogramOverlay(ModuleInfo<LsssIncubatorFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      graphicalInfoLoader = GraphicalInfoLoader.make(getInterpretationSettings().getExecutorObservation(), DataFileSet.empty(), graphicalInfoCache, Utils.emptyConsumer());
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::reset), List.of(
            getEchogramModule().echogramArea(),
            getInterpretationSettings().getDataFileChangeManager()
      ));
      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::readAllGraphicalInfoDatagrams));
      registry.add(getInterpretationSettings().getPingRangeChangeManager(), newCoalescingExecListener(this::setPriorityPingRange));
      registry.add(getInterpretationSettings().getCancelChangeManager(), newCoalescingExecListener(() -> graphicalInfoLoader.cancel()));
      Listener recomputeListener = createRecomputeListener();
      registry.add(graphicalInfoChangeManager, newExecListener(graphicalInfo -> {
         addGraphicalInfo(graphicalInfo);
         recomputeListener.listen();
      }));

      //---

      readAllGraphicalInfoDatagrams();
      reset();
   }

   private void setPriorityPingRange(PingRange pingRange) {
      graphicalInfoLoader.setPriorityPingRange(pingRange);
   }

   private void readAllGraphicalInfoDatagrams() {
      graphicalInfoLoader.cancel();
      graphicalInfos.clear();

      graphicalInfoLoader = GraphicalInfoLoader.make(getInterpretationSettings().getExecutorObservation(),
            getInterpretationSettings().getDataFileSet(), graphicalInfoCache, newExecListener(graphicalInfo -> {
               graphicalInfos.add(graphicalInfo);
               addGraphicalInfo(graphicalInfo);
               graphicalInfoChangeManager.notifyListeners(graphicalInfo);
            }));
      graphicalInfoLoader.setPriorityPingRange(getInterpretationSettings().getPingRange());
   }

   private void addGraphicalInfo(GraphicalInfo graphicalInfo) {
      if (!getInterpretationSettings().getPingRange().intersects(graphicalInfo.pingRange())
            || graphicalInfoPaths.containsKey(graphicalInfo)) {
         return;
      }

      List<RenderedInfo> renderedInfos = graphicalInfo.echogramGraphicalInfos().stream()
            .map(echogramGraphicalInfo -> {
               return echogramGraphicalInfo.render(getPingSettings(), getZSettings(), getEchogramModule().getBounds());
            })
            .toList();
      graphicalInfoPaths.put(graphicalInfo, renderedInfos);
   }

   private void reset() {
      graphicalInfoPaths.clear();
      for (GraphicalInfo graphicalInfo : graphicalInfos) {
         addGraphicalInfo(graphicalInfo);
      }
      recompute();
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      if (graphicalInfoPaths.isEmpty()) {
         return null;
      }
      List<RenderedInfo> renderedInfos = graphicalInfoPaths.values().stream()
            .flatMap(List::stream)
            .toList();
      return transformed(new DisplayData(renderedInfos));
   }

   @Override
   public @Nullable String getToolTipText(Point point) {
      RenderedInfo renderedInfo = activeRenderedInfo;
      if (renderedInfo == null) {
         return null;
      }
      return renderedInfo.texts().stream()
            .map(HtmlEscapers.htmlEscaper().asFunction())
            .collect(Collectors.joining("<br>", "<html>", ""));
   }

   private final class DisplayData implements OverlayDisplayData {
      private final List<RenderedInfo> renderedInfos;

      private DisplayData(List<RenderedInfo> renderedInfos) {
         this.renderedInfos = renderedInfos;
      }

      @Override
      public void draw(Graphics2D g2d) {
         for (RenderedInfo renderedInfo : renderedInfos) {
            renderedInfo.draw(g2d);
         }
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         for (RenderedInfo renderedInfo : renderedInfos) {
            if (renderedInfo.intersects(rectangle)) {
               activeRenderedInfo = renderedInfo;
               return true;
            }
         }
         activeRenderedInfo = null;
         return false;
      }
   }
}
