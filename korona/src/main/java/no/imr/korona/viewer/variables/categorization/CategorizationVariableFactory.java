package no.imr.korona.viewer.variables.categorization;

import no.imr.korona.viewer.variables.VariableCollection;
import no.imr.korona.viewer.variables.VariableFactory;
import no.imr.korona.viewer.variables.VariableGroup;

import java.util.List;

public final class CategorizationVariableFactory implements VariableFactory {
   public static final VariableGroup CATEGORIZATION_VARIABLE_GROUP = new VariableGroup("Categorization");

   public CategorizationVariableFactory() {
   }

   @Override
   public VariableCollection createVariableCollection() {
      CategoryVariable categoryVariable = new CategoryVariable();
      ProbabilityVariable probabilityVariable = new ProbabilityVariable(categoryVariable);
      categoryVariable.setProbabilityVariable(probabilityVariable);

      return new VariableCollection(
            List.of(
                  categoryVariable
            ),
            List.of(
                  probabilityVariable,
                  new DiscriminantVariable(categoryVariable),
                  new DoubtVariable()
            )
      );
   }
}
