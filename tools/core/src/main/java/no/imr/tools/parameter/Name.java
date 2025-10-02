package no.imr.tools.parameter;

import org.jspecify.annotations.Nullable;

/**
 * Naming of objects with both persistent name and display name.
 */
public record Name(
      String persistentName,
      String displayName
) {
   public Name(String persistentName) {
      this(persistentName, persistentName);
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Name that
            && persistentName.equals(that.persistentName);
   }

   @Override
   public int hashCode() {
      return persistentName.hashCode();
   }
}
