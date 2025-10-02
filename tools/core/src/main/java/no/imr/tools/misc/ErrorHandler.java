package no.imr.tools.misc;

import no.imr.tools.logging.Log;

import java.util.logging.Level;

/**
 * Error handler.
 */
@FunctionalInterface
public interface ErrorHandler {
   void onError(String message, Exception e);

   default void onError(Exception e) {
      onError(e.getMessage(), e);
   }

   static ErrorHandler logging() {
      return (message, e) -> Log.global.log(Level.WARNING, message, e);
   }
}
