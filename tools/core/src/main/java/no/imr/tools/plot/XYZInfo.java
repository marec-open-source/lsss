package no.imr.tools.plot;

public record XYZInfo(ParameterExport x, ParameterExport y, ParameterExport z) {

   public static XYZInfo getEmpty() {
      ParameterExport empty = ParameterExport.getEmpty();
      return new XYZInfo(empty, empty, empty);
   }

   public boolean isEmpty() {
      return x.isEmpty();
   }
}
