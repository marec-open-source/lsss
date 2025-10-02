package no.imr.lsss.modules.trawl;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.reflog.ActivityType;
import no.imr.lsss.modules.reflog.LogLine;
import no.imr.lsss.modules.reflog.RefLogDataModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class TrawlModule extends BaseViewModule {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final Supplier<RefLogDataModule> refLogDataModule = moduleSupplier(RefLogDataModule.class);

   public TrawlModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(viewHolder.coalescingListener(View::update), List.of(
            getConfigurationManager().getDataConf().getDir(DataConfLSSS.TRAWL_SUB_DIR),
            getConfigurationManager().getAppMiscConf().useEnglish
      ));
      registry.add(getInterpretationSettings().getReloadChangeManager(),
            viewHolder.coalescingListener(View::reload));
      registry.add(refLogDataModule.get().getLogLineClickChangeManager(), newCoalescingExecListener(this::onLogLineClick));

      //---

      viewHolder.ifView(View::update);
   }

   private void createTrawlLogEvents(List<FishStation> stations) {
      Set<ActivityType> trawlActivityTypes = Set.of(ActivityType.PELAGIC_TRAWL, ActivityType.BOTTOM_TRAWL);
      Set<String> refLogStationNumbers = refLogDataModule.get().getLogLines(refLogDataModule.get()).stream()
            .filter(logLine -> trawlActivityTypes.contains(logLine.activityType()))
            .map(LogLine::localStationNumber)
            .collect(Collectors.toUnmodifiableSet());
      refLogDataModule.get().setLogLines(this, TrawlRefLog.createLogLines(stations, refLogStationNumbers));
   }

   private void onLogLineClick(LogLine logLine) {
      if (logLine.activityType() == ActivityType.PELAGIC_TRAWL || logLine.activityType() == ActivityType.BOTTOM_TRAWL) {
         int stationNumber;
         try {
            stationNumber = Integer.parseInt(logLine.localStationNumber());
         } catch (NumberFormatException e) {
            return;
         }
         viewHolder.ifView(view -> {
            List<FishStation> stations = view.trawlGui.getStations();
            for (int i = 0; i < stations.size(); i++) {
               FishStation station = stations.get(i);
               if (station.stationNumber == stationNumber) {
                  view.trawlGui.setStationIndex(i);
                  return;
               }
            }
         });
      }
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private static final class View extends BaseView {
      private final TrawlModule module;
      private final TrawlGui trawlGui = new TrawlGui();

      private View(TrawlModule module) {
         super(module);

         this.module = module;
         update();

         trawlGui.getFileChangeManager().addListener(() -> {
            module.createTrawlLogEvents(trawlGui.getStations());
         });
      }

      private void update() {
         trawlGui.setDirectory(module.getConfigurationManager().getDataConf().getDir(DataConfLSSS.TRAWL_SUB_DIR).getFile());
         trawlGui.setUseEnglish(module.getConfigurationManager().getAppMiscConf().useEnglish.getBooleanValue());
      }

      private void reload() {
         trawlGui.reload();
      }

      @Override
      public JComponent getComponent() {
         return trawlGui.getComponent();
      }

      @Override
      public void addToFloatableModuleMenu(JPopupMenu popupMenu) {
         JMenuItem visualizerItem = MiscIcons.SCATTER_PLOT.on(popupMenu.add("Visualizer dialog..."));
         visualizerItem.addActionListener(e -> new TrawlVisualizerDialog(trawlGui));

         Path file = trawlGui.getFile();
         if (file != null) {
            JMenuItem openItem = popupMenu.add("Open " + file.getFileName());
            openItem.addActionListener(e -> GuiUtils.desktopOpen(file, getComponent()));
         }
      }
   }
}
