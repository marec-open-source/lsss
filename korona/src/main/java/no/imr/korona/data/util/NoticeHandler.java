package no.imr.korona.data.util;

/**
 * Receives non-critical error messages resulting from reading a data file.
 */
@FunctionalInterface
public interface NoticeHandler {
   void addNotice(String notice);

   static NoticeHandler ignore() {
      return _ -> {
      };
   }
}
