package no.imr.korona.data.formats.ek500;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;

import java.util.Collection;
import java.util.List;

/**
 * The EK500 settings for one transducer.
 */
public final class EK500TransducerSettings extends Configurable implements ParameterContainer, Comparable<EK500TransducerSettings> {
   static final String TRANSDUCER_XML = "transducer";

   public final FloatParameter frequency = new FloatParameter(
         new Name("Frequency"),
         0, Unit.KHZ);

   public final FloatParameter soundVelocity = new FloatParameter(
         new Name("SoundVeloity", "Sound velocity"),
         1473, Unit.METER_PER_SECOND);

   public final FloatParameter pulseLength = new FloatParameter(
         new Name("PulseLength", "Pulse length"),
         1e-3f, Unit.SECONDS);

   public final FloatParameter bandWidth = new FloatParameter(
         new Name("BandWidth", "Band width"),
         3800, Unit.HZ);

   public final FloatParameter transmitPower = new FloatParameter(
         new Name("TransmitPower", "Transmit power"),
         2000, Unit.WATT);

   public final FloatParameter absorptionCoefficient = new FloatParameter(
         new Name("AbsorptionCoefficient", "Absorption coefficient"),
         10e-3f, Unit.DB_PER_METER);

   public final FloatParameter gain = new FloatParameter(
         new Name("Gain"),
         24.3f, Unit.DB);

   public final FloatParameter equivalentBeamAngle = new FloatParameter(
         new Name("EquivalentBeamAngle", "Equivalent beam angle"),
         -20.5f, Unit.DB);

   public EK500TransducerSettings() {
      super(new Name(TRANSDUCER_XML));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            frequency,
            soundVelocity,
            pulseLength,
            bandWidth,
            transmitPower,
            absorptionCoefficient,
            gain,
            equivalentBeamAngle
      );
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return List.of(new ParameterCollection(this));
   }

   @Override
   public int compareTo(EK500TransducerSettings other) {
      return Float.compare(frequency.getFloatValue(), other.frequency.getFloatValue());
   }
}
