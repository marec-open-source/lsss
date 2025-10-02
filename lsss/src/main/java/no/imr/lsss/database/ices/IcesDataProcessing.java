package no.imr.lsss.database.ices;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueParameter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class IcesDataProcessing extends IcesGroup {
   // <xsd:element name="SoftwareName" type="IDREFType"/>
   public final IdRefParameter softwareName = new IdRefParameter(new Name("SoftwareName"),
         "DataProcessingSoftwareName");

   // <xsd:element name="SoftwareVersion" type="nonEmptyString"/>
   public final StringParameter softwareVersion = new StringParameter(new Name("SoftwareVersion"));

   // <xsd:element name="TriwaveCorrection"  type="IDREFType"/>
   public final IdRefParameter triwaveCorrection = new IdRefParameter(new Name("TriwaveCorrection"));

   // <xsd:element name="ChannelID" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter channelID = new StringParameter(new Name("ChannelID"));

   // <xsd:element name="Bandwidth" type="float" minOccurs="0" nillable="true"/>
   public final OptionalFloatParameter bandwidth = new OptionalFloatParameter(new Name("Bandwidth"),
         Optional.empty(), Unit.KHZ);

   // <xsd:element name="Frequency" type="float"/>
   public final FloatParameter frequency = new FloatParameter(new Name("Frequency"),
         0, Unit.KHZ);

   // <xsd:element name="TransceiverPower" type="float"/>
   public final FloatParameter transceiverPower = new FloatParameter(new Name("TransceiverPower"),
         0, Unit.WATT);

   // <xsd:element name="TransmitPulseLength" type="float"/>
   public final FloatParameter transmitPulseLength = new FloatParameter(new Name("TransmitPulseLength"),
         0, Unit.MILLISECONDS);

   // <xsd:element name="OnAxisGain" type="float"/>
   public final FloatParameter onAxisGain = new FloatParameter(new Name("OnAxisGain"),
         0, Unit.DB);

   // <xsd:element name="OnAxisGainUnit" type="IDREFType"/>
   public final IdRefParameter onAxisGainUnit = new IdRefParameter(new Name("OnAxisGainUnit"));

   // <xsd:element name="SaCorrection" type="float" minOccurs="0" nillable="true"/>
   public final OptionalFloatParameter saCorrection = new OptionalFloatParameter(new Name("SaCorrection"),
         Optional.empty(), Unit.DB);

   // <xsd:element name="Absorption" type="float" minOccurs="0" nillable="true"/>
   public final OptionalFloatParameter absorption = new OptionalFloatParameter(new Name("Absorption"),
         Optional.empty(), Unit.DB_PER_METER);

   // <xsd:element name="AbsorptionDescription" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter absorptionDescription = new StringParameter(new Name("AbsorptionDescription"));

   // <xsd:element name="SoundSpeed" type="float" minOccurs="0" nillable="true"/>
   public final OptionalFloatParameter soundSpeed = new OptionalFloatParameter(new Name("SoundSpeed"),
         Optional.empty(), Unit.METER_PER_SECOND);

   // <xsd:element name="SoundSpeedDescription" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter soundSpeedDescription = new StringParameter(new Name("SoundSpeedDescription"));

   // <xsd:element name="TransducerPSI" type="float"/>
   public final FloatParameter transducerPSI = new FloatParameter(new Name("TransducerPSI"),
         0, Unit.DB);

   // <xsd:element name="Comments" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter comments = new StringParameter(new Name("Comments"));

   public IcesDataProcessing() {
      super(new Name("DataProcessing"));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            softwareName,
            softwareVersion,
            triwaveCorrection,
            channelID,
            bandwidth,
            frequency,
            transceiverPower,
            transmitPulseLength,
            onAxisGain,
            onAxisGainUnit,
            saCorrection,
            absorption,
            absorptionDescription,
            soundSpeed,
            soundSpeedDescription,
            transducerPSI,
            comments
      );
   }

   @Override
   public Set<ValueParameter<?>> getMandatoryParameters() {
      return Set.of(
            softwareName,
            softwareVersion,
            triwaveCorrection,
            frequency,
            transceiverPower,
            transmitPulseLength,
            onAxisGain,
            onAxisGainUnit,
            transducerPSI
      );
   }
}
