package no.imr.tools.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A log handler that records max log level.
 */
public abstract class MaxLevelHandler extends HandlerAdapter {
   private Level maxLevel = Level.ALL;

   protected MaxLevelHandler() {
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
         maxLevel = level;
         newMaxLevel(record);
      }
   }

   protected abstract void newMaxLevel(LogRecord record);
}
