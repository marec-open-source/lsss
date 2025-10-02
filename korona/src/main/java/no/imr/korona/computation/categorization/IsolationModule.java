package no.imr.korona.computation.categorization;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;

import java.util.List;

/**
 * Isolates one category by zeroing out all pixels of different categories.
 */
public final class IsolationModule extends ConcurrentPingModule {
   final StringParameter category = new StringParameter(new Name("Category"),
         "",
         "The name of the category to isolate");

   public IsolationModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            category
      );
   }

   void setAvailableCategories(List<String> categories) {
      category.setSuggestedValues(categories);
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
      return new IsolationModuleComputation(this, computationContext, pingSource);
   }
}
