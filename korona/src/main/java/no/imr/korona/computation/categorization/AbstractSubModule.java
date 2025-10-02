package no.imr.korona.computation.categorization;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Contains some functionality useful for submodules.
 */
abstract class AbstractSubModule implements CategorizationSubModule {
   private final CategorizationSubModule previousSubModule;

   AbstractSubModule(CategorizationSubModule previousSubModule) {
      this.previousSubModule = previousSubModule;
   }

   /**
    * Returns an ExtendedPing from the previous module.
    *
    * @return the next ExtendedPing, or {@code null} of end of input
    * @throws IOException if some IO error occurs
    */
   @Nullable ExtendedPing inputExtendedPing() throws IOException {
      return previousSubModule.nextExtendedPing();
   }
}
