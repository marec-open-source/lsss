package no.imr.korona.viewer.variables;

public abstract class VariableFactory {
   protected VariableFactory() {
   }

   public abstract VariableCollection createVariableCollection();
}
