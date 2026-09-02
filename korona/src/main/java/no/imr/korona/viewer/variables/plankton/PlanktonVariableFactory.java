package no.imr.korona.viewer.variables.plankton;

import no.imr.korona.viewer.variables.VariableCollection;
import no.imr.korona.viewer.variables.VariableFactory;
import no.imr.korona.viewer.variables.VariableGroup;

import java.util.List;

public final class PlanktonVariableFactory implements VariableFactory {
   public static final VariableGroup PLANKTON_VARIABLE_GROUP = new VariableGroup("Plankton");

   public PlanktonVariableFactory() {
   }

   @Override
   public VariableCollection createVariableCollection() {
      return new VariableCollection(
            List.of(
                  new PlanktonVariable()
            ),
            List.of(
                  new ResidualVariable(),
                  //todo new FractionVariable(),
                  new BioVolumeVariable()
            )
      );
   }
}
