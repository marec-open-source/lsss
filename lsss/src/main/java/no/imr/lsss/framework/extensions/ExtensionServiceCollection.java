package no.imr.lsss.framework.extensions;

import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.marec.lsss.api.ApiVersion;
import no.marec.lsss.api.LsssPluginLoader;
import no.marec.lsss.api.internal.Restricted;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class ExtensionServiceCollection {
   private final List<URLClassLoader> pluginClassLoaders = findPluginClassLoaders();

   public ExtensionServiceCollection() {
   }

   public Stream<FeatureService> services() {
      return Stream.concat(
            builtinServices(),
            pluginServices(pluginClassLoaders)
      );
   }

   public void close() {
      for (URLClassLoader pluginClassLoader : pluginClassLoaders) {
         try {
            pluginClassLoader.close();
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error closing " + pluginClassLoader.getName(), e);
         }
      }
   }

   private static Stream<ExtensionFeatureService> builtinServices() {
      try {
         return services(ExtensionFeatureService.class.getClassLoader()).stream();
      } catch (ServiceConfigurationError e) {
         Log.global.log(Level.WARNING, "Error loading plugins", e);
         return Stream.of();
      }
   }

   private static Stream<ExtensionFeatureService> pluginServices(List<URLClassLoader> pluginClassLoaders) {
      return pluginClassLoaders.stream()
            .flatMap(pluginClassLoader -> {
               try {
                  return services(pluginClassLoader).stream();
               } catch (Exception | ServiceConfigurationError e) {
                  Log.global.log(Level.WARNING, "Error loading for plugins from " + pluginClassLoader.getName(), e);
                  return Stream.of();
               }
            });
   }

   private static List<URLClassLoader> findPluginClassLoaders() {
      Path pluginsRootDir = LSSS.getApplicationDataDir().resolve("plugins");
      return pluginDirs(pluginsRootDir).stream()
            .map(FileInfo::file)
            .<URLClassLoader>mapMulti((pluginDir, consumer) -> {
               try {
                  Log.global.info("Loading plugin from " + pluginDir);
                  Path libDir = pluginDir.resolve("lib");
                  List<Path> jarFiles = FileUtils.listFiles(libDir, FilePredicates.endsWith(".jar"));
                  URL[] urls = toUrls(jarFiles);
                  consumer.accept(new URLClassLoader("Plugin-" + pluginDir.getFileName(), urls, ExtensionFeatureService.class.getClassLoader()));
               } catch (Exception | ServiceConfigurationError e) {
                  Log.global.log(Level.WARNING, "Error loading for plugins from " + pluginDir, e);
               }
            })
            .toList();
   }

   private static URL[] toUrls(List<Path> files) {
      return files.stream()
            .<URL>mapMulti((file, consumer) -> {
               try {
                  consumer.accept(file.toUri().toURL());
               } catch (MalformedURLException e) {
                  Log.global.log(Level.WARNING, "No URL for " + file, e);
               }
            })
            .toArray(URL[]::new);
   }

   private static List<FileInfo> pluginDirs(Path pluginsRootDir) {
      try {
         return FileUtils.listFilesWithAttributes(pluginsRootDir, FileInfo::isDirectory);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error searching for plugins in " + pluginsRootDir, e);
         return List.of();
      }
   }

   private static List<ExtensionFeatureService> services(ClassLoader classLoader) {
      return ServiceLoader.load(LsssPluginLoader.class, classLoader).stream()
            .filter(provider -> provider.type().getClassLoader() == classLoader)
            .map(ServiceLoader.Provider::get)
            .filter(lsssPluginLoader -> {
               if (lsssPluginLoader instanceof Restricted restricted && !restricted.canBeUsed()) {
                  return false;
               }
               ApiVersion requiredVersion = lsssPluginLoader.getCompatibleApiVersion();
               if (requiredVersion.major() != ApiVersion.CURRENT_MAJOR_VERSION
                     || requiredVersion.minor() > ApiVersion.CURRENT_MINOR_VERSION) {
                  Log.global.log(Level.WARNING, "Cannot use LSSS plugin " + lsssPluginLoader.getId() + " with version " + requiredVersion);
                  return false;
               }
               return true;
            })
            .map(ExtensionFeatureService::new)
            .toList();
   }
}
