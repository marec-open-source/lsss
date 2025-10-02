package no.imr.tools.logging;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import no.imr.tools.Utils;
import no.imr.tools.adm.AppEvent;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;

final class AppEventErrorHandler extends HandlerAdapter {
   private static final int MAX_CACHE_SIZE = 10;

   private final LoggingManager loggingManager;
   private final Cache<String, Boolean> cache = CacheBuilder.newBuilder()
         .expireAfterWrite(5, TimeUnit.MINUTES)
         .build();

   AppEventErrorHandler(LoggingManager loggingManager) {
      this.loggingManager = loggingManager;
   }

   @Override
   public void publish(LogRecord record) {
      Level level = record.getLevel();
      if (level.intValue() >= Log.SILENT_WARNING.intValue()) {
         submit(record);
      }
   }

   private void submit(LogRecord record) {
      String key = record.getSourceClassName() + record.getSourceMethodName() + record.getMessage() +
            (record.getThrown() != null ? Utils.stackTraceToString(record.getThrown()) : "");

      if (cache.size() >= MAX_CACHE_SIZE) {
         return;
      }
      if (cache.getIfPresent(key) != null) {
         return;
      }
      cache.put(key, true);

      String text = new OneLineFormatter().format(record);
      loggingManager.getAppEventHandler().handle(AppEvent.create(loggingManager, AppEvent.Type.error, text));
   }
}
