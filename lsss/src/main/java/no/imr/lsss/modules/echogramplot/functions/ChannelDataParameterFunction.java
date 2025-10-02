package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.util.ExportRounding;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

import java.util.function.ToDoubleFunction;

public final class ChannelDataParameterFunction extends PingFunction {
   private final ToDoubleFunction<ChannelData> function;

   private ChannelDataParameterFunction(Name name, Unit unit, ExportTransform exportTransform, ToDoubleFunction<ChannelData> function) {
      super(name, unit, exportTransform, true);

      this.function = function;
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      ChannelData channelData = ping.getChannelData(channel);
      if (channelData == null) {
         return Double.NaN;
      }
      return function.applyAsDouble(channelData);
   }

   public static ChannelDataParameterFunction absorption() {
      return new ChannelDataParameterFunction(
            new Name("absorption", "Raw: Absorption"),
            Unit.DB_PER_METER, ExportRounding.absorption(),
            ChannelData::getAbsorptionCoefficient);
   }

   public static ChannelDataParameterFunction frequencyStart() {
      return new ChannelDataParameterFunction(
            new Name("frequencyStart", "Raw: Frequency start"),
            Unit.HZ, ExportTransform.identity(),
            ChannelData::getStartFrequency);
   }

   public static ChannelDataParameterFunction frequencyEnd() {
      return new ChannelDataParameterFunction(
            new Name("frequencyEnd", "Raw: Frequency end"),
            Unit.HZ, ExportTransform.identity(),
            ChannelData::getEndFrequency);
   }

   public static ChannelDataParameterFunction gain() {
      return new ChannelDataParameterFunction(
            new Name("gain", "Raw: Gain"),
            Unit.DB, ExportRounding.db(),
            channelData -> channelData.getTransducer().getGainForPulseDuration(channelData.getPulseDuration()));
   }

   public static ChannelDataParameterFunction heave() {
      return new ChannelDataParameterFunction(
            new Name("heave", "Raw: Heave"),
            Unit.METER, ExportRounding.depth(),
            ChannelData::getHeave);
   }

   public static ChannelDataParameterFunction pitch() {
      return new ChannelDataParameterFunction(
            new Name("pitch", "Raw: Pitch"),
            Unit.DEGREES, ExportRounding.degrees(),
            ChannelData::getPitch);
   }

   public static ChannelDataParameterFunction pulseDuration() {
      return new ChannelDataParameterFunction(
            new Name("pulseDuration", "Raw: Pulse duration"),
            Unit.SECONDS, ExportTransform.round(1000_000),
            ChannelData::getPulseDuration);
   }

   public static ChannelDataParameterFunction roll() {
      return new ChannelDataParameterFunction(
            new Name("roll", "Raw: Roll"),
            Unit.DEGREES, ExportRounding.degrees(),
            ChannelData::getRoll);
   }

   public static ChannelDataParameterFunction sampleInterval() {
      return new ChannelDataParameterFunction(
            new Name("sampleInterval", "Raw: Sample interval"),
            Unit.SECONDS, ExportTransform.round(1_000_000),
            ChannelData::getSampleInterval);
   }

   public static ChannelDataParameterFunction soundVelocity() {
      return new ChannelDataParameterFunction(
            new Name("soundVelocity", "Raw: Sound velocity"),
            Unit.METER_PER_SECOND, ExportTransform.round(1000),
            ChannelData::getSoundVelocity);
   }

   public static ChannelDataParameterFunction transducerDepth() {
      return new ChannelDataParameterFunction(
            new Name("transducerDepth", "Raw: Transducer depth"),
            Unit.METER, ExportRounding.depth(),
            ChannelData::getTransducerDepth);
   }

   public static ChannelDataParameterFunction transmitPower() {
      return new ChannelDataParameterFunction(
            new Name("transmitPower", "Raw: Transmit power"),
            Unit.WATT, ExportTransform.round(100),
            ChannelData::getTransmitPower);
   }
}
