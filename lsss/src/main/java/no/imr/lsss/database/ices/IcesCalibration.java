package no.imr.lsss.database.ices;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.DateParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.ValueParameter;

import java.util.List;
import java.util.Set;

public final class IcesCalibration extends IcesGroup {
   // <xsd:element name="Date" type="ISO8601DateType"/>
   public final DateParameter date = new DateParameter(new Name("Date"));

   // <xsd:element name="AcquisitionMethod" type="IDREFType"/>
   public final IdRefParameter acquisitionMethod = new IdRefParameter(new Name("AcquisitionMethod"));

   // <xsd:element name="ProcessingMethod" type="IDREFType"/>
   public final IdRefParameter processingMethod = new IdRefParameter(new Name("ProcessingMethod"));

   // <xsd:element name="AccuracyEstimate" type="nonEmptyString"/>
   public final StringParameter accuracyEstimate = new StringParameter(new Name("AccuracyEstimate"));

   // <xsd:element name="Report" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter report = new StringParameter(new Name("Report"));

   // <xsd:element name="Comments" type="string" minOccurs="0" nillable="true"/>
   public final StringParameter comments = new StringParameter(new Name("Comments"));

   public IcesCalibration() {
      super(new Name("Calibration"));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            date,
            acquisitionMethod,
            processingMethod,
            accuracyEstimate,
            report,
            comments
      );
   }

   @Override
   public Set<ValueParameter<?>> getMandatoryParameters() {
      return Set.of(
            date,
            acquisitionMethod,
            processingMethod,
            accuracyEstimate
      );
   }
}
