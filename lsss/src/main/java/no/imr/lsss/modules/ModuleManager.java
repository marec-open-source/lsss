package no.imr.lsss.modules;

import no.imr.lsss.framework.PluginManager;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages all modules.
 */
public final class ModuleManager {
   private final List<BaseLsssModule> modules = new ArrayList<>();
   private final Map<Where, List<BaseViewModule>> viewModules = new EnumMap<>(Where.class);

   private List<BaseLsssModule> enabledOnStartupModules = Collections.synchronizedList(new ArrayList<>());
   private List<BaseModuleOverlay> enabledOnStartupOverlays = Collections.synchronizedList(new ArrayList<>());

   public ModuleManager(PluginManager pluginManager) {
      List<? extends ModuleCollection<?>> moduleCollections = pluginManager.getFeaturePlugins().parallelStream()
            .map(FeaturePlugin::getModules)
            .toList();

      List<BaseDataModule> dataModules = createDataModules(moduleCollections);
      modules.addAll(dataModules);

      for (Where where : Where.values()) {
         viewModules.put(where, createViewModules(moduleCollections, where));
      }
      viewModules.values().forEach(modules::addAll);

      List<? extends List<? extends BaseModuleOverlay>> overlays = moduleCollections.parallelStream()
            .flatMap(moduleCollection -> moduleCollection.overlays().stream())
            .map(overlayInfoCollection -> overlayInfoCollection.overlaidModuleClass)
            .distinct()
            .flatMap(overlaidModuleClass -> createOverlays(moduleCollections, overlaidModuleClass))
            .toList();
      overlays.forEach(modules::addAll);
   }

   public List<BaseLsssModule> getModules() {
      return modules;
   }

   public Map<Where, List<BaseViewModule>> getViewModules() {
      return viewModules;
   }

   public <T extends BaseLsssModule> T getModule(Class<T> clazz) {
      return Utils.getFirstOrThrow(modules, clazz);
   }

   public <T extends BaseLsssModule> Stream<T> getModules(Class<T> clazz) {
      return getAll(clazz);
   }

   public <T> Stream<T> getAll(Class<T> clazz) {
      return Utils.getAllOfType(modules, clazz);
   }

   public void setup() {
      enabledOnStartupModules.forEach(module -> module.setEnabled(true));
      enabledOnStartupModules = List.of();

      enabledOnStartupOverlays.forEach(overlay -> overlay.setEnabledByUser(true));
      enabledOnStartupOverlays = List.of();
   }

   public void close() {
      modules.forEach(BaseLsssModule::close);
   }

   private static <P extends ModuleInfo.PositionedModuleInfo<?>> void sortModules(List<P> positionedModuleInfos) {
      for (int i = 0; i < positionedModuleInfos.size(); i++) {
         P positionedModuleInfo = positionedModuleInfos.get(i);
         ModuleInfo.RelativePosition relativePosition = positionedModuleInfo.relativePosition();
         if (relativePosition != null) {
            int newPos = -1;
            for (int j = i - 1; j >= 0; j--) {
               if (relativePosition.referenceName().equals(positionedModuleInfos.get(j).moduleInfo().name().persistentName())) {
                  newPos = relativePosition.position() == Position.BEFORE ? j : j + 1;
                  break;
               }
            }
            if (newPos != -1) {
               positionedModuleInfos.add(newPos, positionedModuleInfos.remove(i));
            } else {
               Log.global.warning("Did not find reference " + relativePosition.referenceName() + " for " + positionedModuleInfo.moduleInfo().name().persistentName());
            }
         }
      }
   }

   private List<BaseDataModule> createDataModules(List<? extends ModuleCollection<?>> moduleCollections) {
      return moduleCollections.parallelStream()
            .flatMap(moduleCollection -> moduleCollection.dataModules().dataModuleInfos.stream())
            .<BaseDataModule>mapMulti((dataModuleInfo, consumer) -> {
               BaseDataModule module;
               try {
                  module = dataModuleInfo.create();
               } catch (Exception e) {
                  Log.global.log(Level.WARNING, "Error creating module " + dataModuleInfo.moduleInfo().name().persistentName(), e);
                  return;
               }
               if (dataModuleInfo.moduleInfo().onStartup() == OnStartup.ENABLED) {
                  enabledOnStartupModules.add(module);
               }
               consumer.accept(module);
            })
            .toList();
   }

   private List<BaseViewModule> createViewModules(List<? extends ModuleCollection<?>> moduleCollections, Where where) {
      List<ModuleInfo.ViewModuleInfo<?>> viewModuleInfos = moduleCollections.stream()
            .flatMap(moduleCollection -> moduleCollection.viewModules(where).viewModuleInfos.stream())
            .collect(Collectors.toCollection(ArrayList::new));
      sortModules(viewModuleInfos);
      return viewModuleInfos.parallelStream()
            .<BaseViewModule>mapMulti((viewModuleInfo, consumer) -> {
               BaseViewModule module;
               try {
                  module = viewModuleInfo.create();
               } catch (Exception e) {
                  Log.global.log(Level.WARNING, "Error creating module " + viewModuleInfo.moduleInfo().name().persistentName(), e);
                  return;
               }
               if (viewModuleInfo.moduleInfo().onStartup() == OnStartup.ENABLED) {
                  enabledOnStartupModules.add(module);
               }
               consumer.accept(module);
            })
            .toList();
   }

   private <M extends BaseOverlaidModule<O>, O extends BaseModuleOverlay> Stream<List<O>> createOverlays(
         List<? extends ModuleCollection<?>> moduleCollections, Class<M> overlaidModuleClass) {

      List<ModuleInfo.OverlayInfo<?, M, O>> overlayInfos = moduleCollections.parallelStream()
            .map(moduleCollection -> moduleCollection.overlaysOrNull(overlaidModuleClass))
            .filter(Objects::nonNull)
            .flatMap(overlayInfoCollection -> overlayInfoCollection.overlayInfos.stream())
            .collect(Collectors.toCollection(ArrayList::new));
      sortModules(overlayInfos);
      List<M> overlaidModules = Utils.getAllOfType(modules, overlaidModuleClass).toList();
      return overlaidModules.parallelStream()
            .map(overlaidModule -> {
               List<O> overlays = overlayInfos.parallelStream()
                     .<O>mapMulti((overlayInfo, consumer) -> {
                        O overlay;
                        try {
                           overlay = overlayInfo.create(overlaidModule);
                        } catch (Exception e) {
                           Log.global.log(Level.WARNING, "Error creating overlay " + overlayInfo.moduleInfo().name().persistentName(), e);
                           return;
                        }
                        if (overlayInfo.moduleInfo().onStartup() == OnStartup.ENABLED) {
                           enabledOnStartupOverlays.add(overlay);
                        }
                        consumer.accept(overlay);
                     })
                     .toList();
               overlaidModule.setOverlays(overlays);
               return overlays;
            });
   }
}
