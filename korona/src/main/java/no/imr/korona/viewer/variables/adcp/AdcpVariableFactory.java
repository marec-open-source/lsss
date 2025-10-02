package no.imr.korona.viewer.variables.adcp;

import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.korona.viewer.variables.VariableCollection;
import no.imr.korona.viewer.variables.VariableFactory;
import no.imr.korona.viewer.variables.VariableGroup;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

import java.util.List;

public final class AdcpVariableFactory extends VariableFactory {
   public static final VariableGroup ADCP_VARIABLE_GROUP = new VariableGroup("ADCP");

   public AdcpVariableFactory() {
   }

   @Override
   public VariableCollection createVariableCollection() {
      ContinuousVariableSettings velocitySettings = new ContinuousVariableSettings(FloatRange.of(-10, 10), FloatRange.of(-5, 5), 0.1, false);
      ContinuousVariableSettings correlationSettings = new ContinuousVariableSettings(FloatRange.of(0, 100), FloatRange.of(0, 100), 1, false);
      ContinuousVariableSettings qualitySettings = new ContinuousVariableSettings(FloatRange.of(0, 100), FloatRange.of(0, 100), 1, false);
      return new VariableCollection(
            List.of(),
            List.of(
                  new AdcpBeamVariable("correlation", correlationSettings, Unit.METER_PER_SECOND, 0),
                  new AdcpBeamVariable("correlation", correlationSettings, Unit.METER_PER_SECOND, 1),
                  new AdcpBeamVariable("correlation", correlationSettings, Unit.METER_PER_SECOND, 2),
                  new AdcpBeamVariable("correlation", correlationSettings, Unit.METER_PER_SECOND, 3),

                  new AdcpVariable("current_velocity_geographical_down", velocitySettings, Unit.METER_PER_SECOND),
                  new AdcpVariable("current_velocity_geographical_east", velocitySettings, Unit.METER_PER_SECOND),
                  new AdcpVariable("current_velocity_geographical_north", velocitySettings, Unit.METER_PER_SECOND),

                  new AdcpVariable("current_velocity_vessel_x", velocitySettings, Unit.METER_PER_SECOND),
                  new AdcpVariable("current_velocity_vessel_y", velocitySettings, Unit.METER_PER_SECOND),
                  new AdcpVariable("current_velocity_vessel_z", velocitySettings, Unit.METER_PER_SECOND),

                  new AdcpVariable("quality", qualitySettings, Unit.PERCENT),

                  new AdcpBeamVariable("velocity", velocitySettings, Unit.METER_PER_SECOND, 0),
                  new AdcpBeamVariable("velocity", velocitySettings, Unit.METER_PER_SECOND, 1),
                  new AdcpBeamVariable("velocity", velocitySettings, Unit.METER_PER_SECOND, 2),
                  new AdcpBeamVariable("velocity", velocitySettings, Unit.METER_PER_SECOND, 3),

                  new AdcpVariable("Mean_current/current_velocity_geographical_down", velocitySettings, Unit.METER_PER_SECOND),
                  new AdcpVariable("Mean_current/current_velocity_geographical_east", velocitySettings, Unit.METER_PER_SECOND),
                  new AdcpVariable("Mean_current/current_velocity_geographical_north", velocitySettings, Unit.METER_PER_SECOND),

                  new AdcpVariable("Mean_current/current_velocity_vessel_x", velocitySettings, Unit.METER_PER_SECOND),
                  new AdcpVariable("Mean_current/current_velocity_vessel_y", velocitySettings, Unit.METER_PER_SECOND),
                  new AdcpVariable("Mean_current/current_velocity_vessel_z", velocitySettings, Unit.METER_PER_SECOND),

                  new AdcpVariable("Mean_current/quality", qualitySettings, Unit.PERCENT)
            )
      );
   }
}
