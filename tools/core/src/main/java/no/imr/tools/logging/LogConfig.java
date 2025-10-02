package no.imr.tools.logging;

/**
 * For configuring logging using system property.
 * <blockquote>
 * {@code -Djava.util.logging.config.class=no.imr.tools.logging.LogConfig}
 * </blockquote>
 */
public record LogConfig() {
   private static boolean done;

   public LogConfig {
      Log.init();
      done = true;
   }

   static boolean isDone() {
      return done;
   }
}
