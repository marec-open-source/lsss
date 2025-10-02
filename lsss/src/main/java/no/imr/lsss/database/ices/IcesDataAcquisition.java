package no.imr.lsss.database.ices;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.ValueParameter;

import java.util.List;
import java.util.Set;

public final class IcesDataAcquisition extends IcesGroup {
   // <xsd:element name="SoftwareName" type="IDREFType" minOccurs="0" nillable="true"/>
   public final IdRefParameter softwareName = new IdRefParameter(new Name("SoftwareName"),
         "DataAcquisitionSoftwareName");

   // <xsd:element name="SoftwareVersion" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter softwareVersion = new StringParameter(new Name("SoftwareVersion"));

   // <xsd:element name="StoredDataFormat" type="IDREFType"/>
   public final IdRefParameter storedDataFormat = new IdRefParameter(new Name("StoredDataFormat"));

   // <xsd:element name="PingDutyCycle" type="nonEmptyString"/>
   public final StringParameter pingDutyCycle = new StringParameter(new Name("PingDutyCycle"));

   // <xsd:element name="Comments" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter comments = new StringParameter(new Name("Comments"));

   public IcesDataAcquisition() {
      super(new Name("DataAcquisition"));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            softwareName,
            softwareVersion,
            storedDataFormat,
            pingDutyCycle,
            comments
      );
   }

   @Override
   public Set<ValueParameter<?>> getMandatoryParameters() {
      return Set.of(
            storedDataFormat,
            pingDutyCycle
      );
   }
}
