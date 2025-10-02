package no.imr.korona.computation.categorization;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

abstract class SimpleSubModule extends AbstractSubModule {
   SimpleSubModule(CategorizationSubModule previousSubModule) {
      super(previousSubModule);
   }

   @Override
   public final @Nullable ExtendedPing nextExtendedPing() throws IOException {
      ExtendedPing extendedPing = inputExtendedPing();
      if (extendedPing != null) {
         CategorizationPing categorizationPing = extendedPing.getCategorizationPing();
         if (categorizationPing != null) {
            process(extendedPing, categorizationPing);
         }
      }
      return extendedPing;
   }

   abstract void process(ExtendedPing extendedPing, CategorizationPing categorizationPing);
}
