package no.imr.tools.logging;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import no.imr.tools.ShouldNotHappenException;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public final class LogOnce {
   private static final Cache<String, Object> CACHE = CacheBuilder.newBuilder()
         .weakValues()
         .build();

   private LogOnce() {
   }

   public static void warning(String message, Object weakReferenceObject) {
      log(Level.WARNING, message, weakReferenceObject);
   }

   public static void info(String message, Object weakReferenceObject) {
      log(Level.INFO, message, weakReferenceObject);
   }

   public static void log(Level level, String message, Object weakReferenceObject) {
      try {
         CACHE.get(message, () -> {
            Log.global.log(level, message);
            return weakReferenceObject;
         });
      } catch (ExecutionException e) {
         throw new ShouldNotHappenException(e);
      }
   }
}
