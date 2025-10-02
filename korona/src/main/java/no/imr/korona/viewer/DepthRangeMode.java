package no.imr.korona.viewer;

/**
 * Enumeration of all depth range modes.
 */
public enum DepthRangeMode {
   MANUAL("Manual"),
   MAX("Maximum"),
   AUTO("Auto");

   private final String label;

   DepthRangeMode(String label) {
      this.label = label;
   }

   @Override
   public String toString() {
      return label;
   }
}
