package no.imr.tools.adm;

import com.google.common.base.Joiner;
import no.imr.tools.logging.Log;

import java.util.Map;

@FunctionalInterface
public interface AppEventHandler {
   void handle(Map<String, Object> event);

   static AppEventHandler ignore() {
      return _ -> {
      };
   }

   static AppEventHandler logging() {
      return event -> {
         String text = Joiner.on(", ").withKeyValueSeparator("=").join(event);
         Log.global.info("AppEvent: " + text);
      };
   }
}
