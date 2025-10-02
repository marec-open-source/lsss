package no.imr.korona.computation.categorization;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Categorization is done by pulling ExtendedPings through
 * a chain of CategorizationSubModule.
 */
interface CategorizationSubModule {
   /**
    * Returns the next ExtendedPing in the data stream.
    *
    * @return the next processed ExtendedPing, or {@code null} if end of input
    * @throws IOException if some IO error occurs
    */
   @Nullable ExtendedPing nextExtendedPing() throws IOException;
}
