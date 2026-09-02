package no.imr.tools.logging;

import no.imr.tools.Utils;
import no.imr.tools.time.TimeUtils;

import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter for writing each log message on one line.
 */
public final class OneLineFormatter extends Formatter {
   static final DateTimeFormatter DATE_FORMAT = TimeUtils.createLocalDateTimeFormatter("yyyy-MM-dd HH:mm:ss");

   public OneLineFormatter() {
   }

   @Override
   public String format(LogRecord record) {
      return DATE_FORMAT.format(record.getInstant())
            + " ["
            + (record.getLongThreadID() == Thread.currentThread().threadId() ? Thread.currentThread().getName() : record.getLongThreadID())
            + "] "
            + record.getLevel().getName()
            + ' '
            + record.getSourceClassName()
            + '.'
            + record.getSourceMethodName()
            + " - "
            + record.getMessage()
            + '\n'
            + (record.getThrown() != null ? Utils.stackTraceToString(record.getThrown()) : "");
   }
}
