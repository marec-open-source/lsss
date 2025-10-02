package no.imr.korona.config;

import no.imr.tools.parameter.Name;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

public record ConfigFileSettingsContext(Name name, SvgIcon icon) {
   @Override
   public String toString() {
      return name.persistentName();
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ConfigFileSettingsContext that
            && name.equals(that.name);
   }

   @Override
   public int hashCode() {
      return name.hashCode();
   }
}
