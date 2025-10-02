package no.marec.lsss.api.util.development;

import no.marec.lsss.api.internal.InternalLsss;

/**
 * Main class for starting LSSS during plugin development.
 * <p>
 * All jar files in an LSSS installation must be on the classpath.
 */
public final class DevStartLsssMain {
   private DevStartLsssMain() {
   }

   /**
    * Starts LSSS.
    *
    * @param args command line arguments
    */
   public static void main(String[] args) {
      InternalLsss.INSTANCE.devUtils().startLsss(args);
   }
}
