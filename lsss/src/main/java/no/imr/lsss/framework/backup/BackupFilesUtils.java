package no.imr.lsss.framework.backup;

import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

final class BackupFilesUtils {
   private BackupFilesUtils() {
   }

   static @Nullable Path getLastUsedDestinationDir(Path settingsFile) {
      try {
         String path = Files.readString(settingsFile, Utils.UTF_8).trim();
         return path.isEmpty() ? null : Path.of(path);
      } catch (Exception _) {
         return null;
      }
   }

   static Set<Path> getExcludedSourceDirs(LSSS lsss) {
      return lsss.getPluginManager().getFeaturePlugins().stream()
            .flatMap(plugin -> plugin.getExcludedDirsForCopying().stream())
            .collect(Collectors.toUnmodifiableSet());
   }

   static List<SelectionItem<BackupExclusionOption>> getExclusionOptions(LSSS lsss, Map<String, Boolean> saveOptions) {
      return lsss.getPluginManager().getFeaturePlugins().stream()
            .flatMap(plugin -> plugin.getBackupExclusionOptions().stream())
            .map(option -> {
               boolean selected = saveOptions.getOrDefault(option.name().persistentName(), false);
               return new SelectionItem<>(option, selected);
            })
            .toList();
   }

   static Map<String, Boolean> toSaveOptions(List<SelectionItem<BackupExclusionOption>> exclusionOptions) {
      return exclusionOptions.stream()
            .collect(Collectors.toMap(item -> item.get().name().persistentName(), SelectionItem::isSelected, Boolean::logicalOr, TreeMap::new));
   }
}
