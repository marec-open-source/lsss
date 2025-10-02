package no.imr.korona.viewer.variables;

import java.util.List;

public record VariableCollection(
      List<DiscreteVariable> discreteVariables,
      List<ContinuousVariable> continuousVariables
) {
}
