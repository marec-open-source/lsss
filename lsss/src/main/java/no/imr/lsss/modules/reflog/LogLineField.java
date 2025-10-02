package no.imr.lsss.modules.reflog;

import no.imr.tools.Utils;

public record LogLineField(String name, String unit) {
   String nameAndUnit() {
      return Utils.nameAndUnit(name, unit);
   }

   @Override
   public String toString() {
      return nameAndUnit();
   }
}
