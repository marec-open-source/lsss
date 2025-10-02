package no.imr.lsss.modules.schoolparameter;

import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;

public record SchoolParameter(
      Name name,
      Unit unit
) {
   public String getPersistentName() {
      return name.persistentName();
   }
}
