package no.imr.tools.help;

import no.imr.tools.logging.Log;

@FunctionalInterface
public interface HelpDisplayer extends AutoCloseable {
   void display(HelpID helpID);

   @Override
   default void close() {
   }

   static HelpDisplayer noneAvailable() {
      return helpID -> Log.global.warning("No available help displayer. " + helpID);
   }
}
