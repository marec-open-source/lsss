package no.imr.korona.computation;

import no.imr.korona.plugins.ModulePlugin;
import no.imr.korona.plugins.ModuleService;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.plugins.BaseService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Functionality for locating module classes and creating new instances.
 */
public final class ModuleManager {
   private final List<ModulePlugin> modulePlugins;
   private final Map<String, ModuleInfo> moduleInfos = new HashMap<>();

   public ModuleManager() {
      this(BaseService.getUsableServices(ModuleService.class));
   }

   public ModuleManager(List<ModuleService> services) {
      this(services.stream());
   }

   private ModuleManager(Stream<ModuleService> services) {
      modulePlugins = services
            .map(ModuleService::createPlugin)
            .sorted(Utils.comparingIgnoringCase(plugin -> plugin instanceof KoronaModulePlugin ? "" : plugin.getName().displayName()))
            .toList();
      for (ModulePlugin modulePlugin : modulePlugins) {
         modulePlugin.addModuleInfos(new ModuleInfoCollector(modulePlugin, this));
      }
   }

   void addModuleInfo(ModuleInfo moduleInfo) {
      ModuleInfo old = moduleInfos.put(moduleInfo.getPersistentName(), moduleInfo);
      if (old != null) {
         Log.global.warning("Removed previously registered " + old.getPersistentName() + ':' + old.moduleClass());
      }
   }

   public List<ModulePlugin> getModulePlugins() {
      return modulePlugins;
   }

   public Collection<ModuleInfo> getModuleInfos() {
      return moduleInfos.values();
   }

   public Collection<ModuleInfo> getModuleInfos(ModulePlugin modulePlugin) {
      return getModuleInfos().stream()
            .filter(moduleInfo -> moduleInfo.modulePlugin() == modulePlugin)
            .toList();
   }

   /**
    * Creates a new instance of a specified module.
    *
    * @param persistentName the persistent name of the module
    * @return the new instance
    * @throws ModuleCreationException if the module could not be created
    */
   public BaseModule createModule(String persistentName) throws ModuleCreationException {
      ModuleInfo moduleInfo = moduleInfos.get(persistentName);
      if (moduleInfo == null) {
         throw new ModuleCreationException("Unknown module " + persistentName);
      }
      try {
         BaseModule module = moduleInfo.moduleClass().getDeclaredConstructor().newInstance();
         module.setModuleInfo(moduleInfo);
         return module;
      } catch (Exception e) {
         throw new ModuleCreationException("Error creating module " + persistentName, e);
      }
   }
}
