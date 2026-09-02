package no.imr.tools.logging;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A log handler that records max log level.
 */
public final class MaxLevelHandler extends HandlerAdapter {
   private final Consumer<LogRecord> maxLevelConsumer;
   private volatile Level maxLevel = Level.ALL;

   public MaxLevelHandler(Consumer<LogRecord> maxLevelConsumer) {
      this.maxLevelConsumer = maxLevelConsumer;
   }

   public Level getMaxLevel() {
      return maxLevel;
   }

   public void reset() {
      maxLevel = Level.ALL;
   }

   @Override
   public void publish(LogRecord record) {
      Level level = record.getLevel();
      if (level.intValue() > maxLevel.intValue()) {
         synchronized (this) {
            if (level.intValue() > maxLevel.intValue()) {
               maxLevel = level;
               maxLevelConsumer.accept(record);
            }
         }
      }
   }
}
