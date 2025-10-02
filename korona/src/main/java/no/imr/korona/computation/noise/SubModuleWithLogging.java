package no.imr.korona.computation.noise;

/**
 * Common routines for log handling in noise masks and histograms.
 */
abstract class SubModuleWithLogging {
   private String logLabel = "";

   SubModuleWithLogging() {
   }

   /**
    * Called by the module who owns this submodule.
    *
    * @param logLabel the prefix
    */
   void setLogLabel(String logLabel) {
      this.logLabel = logLabel;
   }

   /**
    * Gets the label to be prefixed to all logging by this submodule.
    *
    * @return the prefix
    */
   String getLogLabel() {
      return logLabel;
   }
}
