package no.imr.tools.plot;

import no.imr.tools.parameter.Unit;

public record ParameterExport(
      String name,
      Unit unit,
      ExportTransform transform
) {
   public static ParameterExport getEmpty() {
      return new ParameterExport("", Unit.NONE, ExportTransform.identity());
   }

   public boolean isEmpty() {
      return name.isEmpty();
   }
}
