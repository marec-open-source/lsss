package no.imr.lsss.framework;

import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Instantiates plugins registered in a {@link ServiceCollection}.
 *
 * @see FeaturePlugin
 */
public final class PluginManager {
   private final List<FeaturePlugin> featurePlugins;

   public PluginManager(LSSS lsss, ServiceCollection serviceCollection) {
      featurePlugins = serviceCollection.getFeatureServices().parallelStream()
            .map(service -> {
               try {
                  return service.createPlugin(lsss);
               } catch (Throwable e) {
                  Log.global.log(Level.WARNING, "Error creating plugin " + service.getName().persistentName(), e);
                  return null;
               }
            })
            .filter(Objects::nonNull)
            .toList();
      String message = featurePlugins.stream()
            .map(FeaturePlugin::getPersistentName)
            .collect(Collectors.joining(", ", "Plugins: ", ""));
      Log.global.fine(message);
   }

   public void setup() {
      featurePlugins.forEach(FeaturePlugin::setup);
   }

   public void close() {
      featurePlugins.forEach(FeaturePlugin::close);
   }

   public List<FeaturePlugin> getFeaturePlugins() {
      return featurePlugins;
   }

   public <T extends FeaturePlugin> T getFeaturePlugin(Class<T> clazz) {
      return Utils.getFirstOrThrow(featurePlugins, clazz);
   }
}
