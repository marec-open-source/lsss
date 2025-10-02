package no.imr.tools.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Adapter class for {@link Handler}.
 */
public abstract class HandlerAdapter extends Handler {
   protected HandlerAdapter() {
   }

   @Override
   public void publish(LogRecord record) {
   }

   @Override
   public void flush() {
   }

   @Override
   public void close() {
   }
}
