package no.imr.korona.region;

public interface InterpretationContainer {
   Interpretation getInterpretation();

   default boolean isReadOnly() {
      return false;
   }

   default boolean isWritable() {
      return !isReadOnly();
   }
}
