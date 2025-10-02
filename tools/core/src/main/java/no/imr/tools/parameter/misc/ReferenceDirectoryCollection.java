package no.imr.tools.parameter.misc;

import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * A collection of reference directories.
 */
public final class ReferenceDirectoryCollection extends Configurable {
   private final Map<String, ReferenceDirectory> referenceDirectories = new TreeMap<>();

   public ReferenceDirectoryCollection(Name name, ReferenceDirectory referenceDirectory) {
      super(name);

      addReferenceDirectory(referenceDirectory);
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return referenceDirectories.values();
   }

   @Override
   protected Configurable possiblyCreateNewSubConfigurable(String persistentName) {
      ReferenceDirectory referenceDirectory = new ReferenceDirectory(new Name(persistentName));
      addReferenceDirectory(referenceDirectory);
      return referenceDirectory;
   }

   public void addReferenceDirectory(ReferenceDirectory referenceDirectory) {
      referenceDirectories.put(referenceDirectory.getName().persistentName(), referenceDirectory);
   }

   public @Nullable ReferenceDirectory getReferenceDirectory(String persistentName) {
      return referenceDirectories.get(persistentName);
   }

   public @Nullable ReferenceDirectory getReferenceDirectory(Path file) {
      for (ReferenceDirectory referenceDirectory : referenceDirectories.values()) {
         Path dir = referenceDirectory.getFile();
         if (dir != null && FileUtils.isInDir(file, dir)) {
            return referenceDirectory;
         }
      }
      return null;
   }
}
