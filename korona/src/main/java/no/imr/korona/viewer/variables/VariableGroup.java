package no.imr.korona.viewer.variables;

public record VariableGroup(String name) {
   @Override
   public String toString() {
      return name;
   }
}
