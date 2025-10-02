package no.imr.tools.logging;

import no.imr.tools.Utils;

import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter for writing each log message on one line.
 */
public final class OneLineFormatter extends Formatter {
   static final DateTimeFormatter DATE_FORMAT = Utils.createLocalDateTimeFormatter("yyyy-MM-dd HH:mm:ss");

   public OneLineFormatter() {
   }

   @Override
   public String format(LogRecord record) {
      StringBuilder sb = new StringBuilder(160)
            .append(DATE_FORMAT.format(record.getInstant()))
            .append(" [")
            .append(record.getLongThreadID() == Thread.currentThread().threadId() ? Thread.currentThread().getName() : record.getLongThreadID())
            .append("] ")
            .append(record.getLevel().getName())
            .append(' ')
            .append(record.getSourceClassName())
            .append('.')
            .append(record.getSourceMethodName())
            .append(" - ")
            .append(record.getMessage())
            .append('\n');
      if (record.getThrown() != null) {
         sb.append(Utils.stackTraceToString(record.getThrown()));
      }
      return sb.toString();
   }
}
