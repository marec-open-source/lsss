package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.variables.adcp.AdcpData;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalStringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdcpFunction extends PingFunction {
   private static final Pattern BEAM_NUMBER_PATTERN = Pattern.compile("(\\w+),\\s*beam\\s*(\\d+)");
   private static final Map<String, Unit> VARIABLE_PATH_TO_UNIT = makeVariablePathToUnit();

   private final String variablePath;
   private final int beamIndex;

   private AdcpFunction(String variablePath) {
      super(new Name("ADCP: " + variablePath), variablePathToUnit(variablePath), ExportTransform.identity());

      Matcher matcher = BEAM_NUMBER_PATTERN.matcher(variablePath);
      if (matcher.matches()) {
         this.variablePath = matcher.group(1);
         beamIndex = Integer.parseInt(matcher.group(2)) - 1; // -1 to convert from beamNumber to beamIndex.
      } else {
         this.variablePath = variablePath;
         beamIndex = -1;
      }

      selected.setBooleanValue(true);
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      return AdcpData.readDouble(variablePath, ping, beamIndex);
   }

   private static Map<String, Unit> makeVariablePathToUnit() {
      Map<String, Unit> map = new LinkedHashMap<>();
      map.put("bin_length", Unit.METER);
      map.put("back_scatter_at_bottom_i, beam 1", Unit.WATT);
      map.put("back_scatter_at_bottom_i, beam 2", Unit.WATT);
      map.put("back_scatter_at_bottom_i, beam 3", Unit.WATT);
      map.put("back_scatter_at_bottom_i, beam 4", Unit.WATT);
      map.put("back_scatter_at_bottom_r, beam 1", Unit.WATT);
      map.put("back_scatter_at_bottom_r, beam 2", Unit.WATT);
      map.put("back_scatter_at_bottom_r, beam 3", Unit.WATT);
      map.put("back_scatter_at_bottom_r, beam 4", Unit.WATT);
      map.put("bottom_track_velocity_vessel_x", Unit.METER_PER_SECOND);
      map.put("bottom_track_velocity_vessel_y", Unit.METER_PER_SECOND);
      map.put("bottom_track_velocity_vessel_z", Unit.METER_PER_SECOND);
      map.put("correlation_factor_limit", Unit.PERCENT);
      map.put("correlation_at_bottom, beam 1", Unit.PERCENT);
      map.put("correlation_at_bottom, beam 2", Unit.PERCENT);
      map.put("correlation_at_bottom, beam 3", Unit.PERCENT);
      map.put("correlation_at_bottom, beam 4", Unit.PERCENT);
      map.put("depth_first_sample_center", Unit.METER);
      map.put("error_velocity_limit", Unit.METER_PER_SECOND);
      map.put("scaling_factor", Unit.METER);
      map.put("slant_range_to_bottom, beam 1", Unit.METER);
      map.put("slant_range_to_bottom, beam 2", Unit.METER);
      map.put("slant_range_to_bottom, beam 3", Unit.METER);
      map.put("slant_range_to_bottom, beam 4", Unit.METER);
      map.put("sv_dbw_high_limit", Unit.DB);
      map.put("sv_dbw_low_limit", Unit.DB);
      map.put("transmit_duration_nominal_sub_pulse", Unit.SECONDS);
      map.put("transmit_lag_interval_sub_pulse", Unit.SECONDS);
      map.put("velocity_depth_stabilization", Unit.NONE);
      map.put("velocity_motion_stabilization", Unit.NONE);
      map.put("vertical_sample_interval", Unit.SECONDS);
      map.put("Mean_current/averaging", Unit.COUNT);
      map.put("Mean_current/bottom_track_velocity_vessel_x", Unit.METER_PER_SECOND);
      map.put("Mean_current/bottom_track_velocity_vessel_y", Unit.METER_PER_SECOND);
      map.put("Mean_current/bottom_track_velocity_vessel_z", Unit.METER_PER_SECOND);
      map.put("Mean_current/mean_bin_length", Unit.METER);
      map.put("Mean_current/mean_platform_heading", Unit.DEGREES);
      map.put("Mean_current/mean_platform_latitude", Unit.DEGREES);
      map.put("Mean_current/mean_platform_longitude", Unit.DEGREES);
      map.put("Mean_current/mean_platform_pitch", Unit.DEGREES);
      map.put("Mean_current/mean_platform_roll", Unit.DEGREES);
      map.put("Mean_current/mean_platform_vertical", Unit.METER);
      map.put("Mean_current/percent_good_limit", Unit.PERCENT);
      return map;
   }

   private static Unit variablePathToUnit(String variablePath) {
      return VARIABLE_PATH_TO_UNIT.getOrDefault(variablePath, Unit.NONE);
   }

   public static final class AdcpParameter extends OptionalStringParameter {
      public AdcpParameter(String persistentName) {
         super(new Name(persistentName, "ADCP"), Optional.empty(), "");
      }

      public static AdcpFunction stringToFunction(String variablePath) {
         return new AdcpFunction(variablePath);
      }

      @Override
      public List<Optional<String>> getSuggestedValues() {
         return VARIABLE_PATH_TO_UNIT.keySet().stream()
               .map(Optional::of)
               .toList();
      }
   }
}
