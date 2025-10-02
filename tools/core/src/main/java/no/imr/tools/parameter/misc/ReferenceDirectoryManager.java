package no.imr.tools.parameter.misc;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReferenceDirectoryManager {
   private final List<ReferenceDirectoryCollection> referenceDirectoryCollections = new ArrayList<>();
   private @Nullable ReferenceDirectory relativeEverywhereReferenceDirectory;

   public ReferenceDirectoryManager() {
   }

   public ReferenceDirectoryManager add(ReferenceDirectoryCollection referenceDirectoryCollection) {
      referenceDirectoryCollections.add(referenceDirectoryCollection);
      return this;
   }

   public ReferenceDirectoryManager setRelativeEverywhereReferenceDirectory(ReferenceDirectory relativeEverywhereReferenceDirectory) {
      this.relativeEverywhereReferenceDirectory = relativeEverywhereReferenceDirectory;
      return this;
   }

   public @Nullable ReferenceDirectory getReferenceDirectory(String persistentName) {
      for (ReferenceDirectoryCollection referenceDirectoryCollection : referenceDirectoryCollections) {
         ReferenceDirectory referenceDirectory = referenceDirectoryCollection.getReferenceDirectory(persistentName);
         if (referenceDirectory != null) {
            return referenceDirectory;
         }
      }

      if (relativeEverywhereReferenceDirectory != null && relativeEverywhereReferenceDirectory.getName().persistentName().equals(persistentName)) {
         return relativeEverywhereReferenceDirectory;
      }

      return null;
   }

   public @Nullable ReferenceDirectory getReferenceDirectory(Path file) {
      for (ReferenceDirectoryCollection referenceDirectoryCollection : referenceDirectoryCollections) {
         ReferenceDirectory referenceDirectory = referenceDirectoryCollection.getReferenceDirectory(file);
         if (referenceDirectory != null) {
            return referenceDirectory;
         }
      }

      if (relativeEverywhereReferenceDirectory != null && relativeEverywhereReferenceDirectory.getFile() != null) {
         return relativeEverywhereReferenceDirectory;
      }

      return null;
   }
}
