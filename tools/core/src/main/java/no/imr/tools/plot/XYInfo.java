package no.imr.tools.plot;

public record XYInfo(ParameterExport x, ParameterExport y) {

   public static XYInfo getEmpty() {
      ParameterExport empty = ParameterExport.getEmpty();
      return new XYInfo(empty, empty);
   }

   public boolean isEmpty() {
      return x.isEmpty();
   }
}
