package no.imr.korona.viewer.variables.raw;

import no.imr.korona.viewer.variables.VariableCollection;
import no.imr.korona.viewer.variables.VariableFactory;
import no.imr.korona.viewer.variables.VariableGroup;

import java.util.List;

public final class RawVariableFactory extends VariableFactory {
   public static final VariableGroup RAW_VARIABLE_GROUP = new VariableGroup("Raw");

   public RawVariableFactory() {
   }

   @Override
   public VariableCollection createVariableCollection() {
      return new VariableCollection(
            List.of(),
            List.of(
                  new SvVariable(),
                  new TsuVariable(),
                  new TscVariable(),
                  new AlongshipAngleVariable(),
                  new AthwartshipAngleVariable(),
                  new HorizontalAngleVariable(),
                  new VerticalAngleVariable(),
                  new RelativeFrequencyResponseVariable(),
                  new RawComplexVariable()
            )
      );
   }
}
