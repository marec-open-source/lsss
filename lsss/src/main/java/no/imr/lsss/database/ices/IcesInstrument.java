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

public final class IcesInstrument extends IcesGroup {
   // <xsd:element name="Frequency" type="float"/>
   public final FloatParameter frequency = new FloatParameter(new Name("Frequency"),
         0, Unit.KHZ);

   // <xsd:element name="TransducerLocation" type="IDREFType"/>
   public final IdRefParameter transducerLocation = new IdRefParameter(new Name("TransducerLocation"));

   // <xsd:element name="TransducerManufacturer" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter transducerManufacturer = new StringParameter(new Name("TransducerManufacturer"));

   // <xsd:element name="TransducerModel" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter transducerModel = new StringParameter(new Name("TransducerModel"));

   // <xsd:element name="TransducerSerial" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter transducerSerial = new StringParameter(new Name("TransducerSerial"));

   // <xsd:element name="TransducerBeamType" type="IDREFType"/>
   public final IdRefParameter transducerBeamType = new IdRefParameter(new Name("TransducerBeamType"));

   // <xsd:element name="TransducerDepth" type="float" minOccurs="0" nillable="true"/>
   public final OptionalFloatParameter transducerDepth = new OptionalFloatParameter(new Name("TransducerDepth"),
         Optional.empty(), Unit.METER);

   // <xsd:element name="TransducerOrientation" type="nonEmptyString"/>
   public final StringParameter transducerOrientation = new StringParameter(new Name("TransducerOrientation"));

   // <xsd:element name="TransducerPSI" type="float" minOccurs="0" nillable="true"/>
   public final OptionalFloatParameter transducerPSI = new OptionalFloatParameter(new Name("TransducerPSI"),
         Optional.empty(), Unit.DB);

   // <xsd:element name="TransducerBeamAngleMajor" type="float" minOccurs="0" nillable="true"/>
   public final OptionalFloatParameter transducerBeamAngleMajor = new OptionalFloatParameter(new Name("TransducerBeamAngleMajor"),
         Optional.empty(), Unit.DEGREES);

   // <xsd:element name="TransducerBeamAngleMinor" type="float" minOccurs="0" nillable="true"/>
   public final OptionalFloatParameter transducerBeamAngleMinor = new OptionalFloatParameter(new Name("TransducerBeamAngleMinor"),
         Optional.empty(), Unit.DEGREES);

   // <xsd:element name="TransceiverManufacturer" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter transceiverManufacturer = new StringParameter(new Name("TransceiverManufacturer"));

   // <xsd:element name="TransceiverModel" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter transceiverModel = new StringParameter(new Name("TransceiverModel"));

   // <xsd:element name="TransceiverSerial" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter transceiverSerial = new StringParameter(new Name("TransceiverSerial"));

   // <xsd:element name="TransceiverFirmware" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter transceiverFirmware = new StringParameter(new Name("TransceiverFirmware"));

   // <xsd:element name="Comments" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter comments = new StringParameter(new Name("Comments"));

   public IcesInstrument() {
      super(new Name("Instrument"));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            frequency,
            transducerLocation,
            transducerManufacturer,
            transducerModel,
            transducerSerial,
            transducerBeamType,
            transducerDepth,
            transducerOrientation,
            transducerPSI,
            transducerBeamAngleMajor,
            transducerBeamAngleMinor,
            transceiverManufacturer,
            transceiverModel,
            transceiverSerial,
            transceiverFirmware,
            comments
      );
   }

   @Override
   public Set<ValueParameter<?>> getMandatoryParameters() {
      return Set.of(
            frequency,
            transducerLocation,
            transducerBeamType,
            transducerOrientation
      );
   }
}
