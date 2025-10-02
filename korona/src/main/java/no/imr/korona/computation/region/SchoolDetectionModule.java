package no.imr.korona.computation.region;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * A module for detecting schools.
 */
public final class SchoolDetectionModule extends SimplePingModule {
   private final HeaderParameter detectionHeader = new HeaderParameter("Detection settings");

   public final BooleanParameter processLast = new BooleanParameter(
         new Name("ProcessLast", "Process last"),
         true,
         "If selected, the last channel will be used for school detection");

   public final IntParameter channel = new IntParameter(
         new Name("Channel"),
         1, Unit.NONE,
         "Which channel to work on, starting at 1");

   public final OptionalFloatParameter minDepth = new OptionalFloatParameter(
         new Name("MinDepth", "Min depth"),
         Optional.empty(), Unit.METER, ValueConstraints.gte(0f),
         "Minimum depth for school detection");

   public final OptionalFloatParameter maxDepth = new OptionalFloatParameter(
         new Name("MaxDepth", "Max depth"),
         Optional.empty(), Unit.METER, ValueConstraints.gte(0f),
         "Maximum depth for school detection");

   public final FloatParameter threshold = new FloatParameter(
         new Name("Threshold"),
         -62f, Unit.DB,
         "Minimum value of all samples inside a school");

   private final HeaderParameter acceptanceHeader = new HeaderParameter("Acceptance settings");

   public final RangeParameter meanSv = new RangeParameter(
         new Name("Density", "Mean Sv"),
         -120, -20, Unit.DB,
         "Average Sv in school");

   public final RangeParameter maxSv = new RangeParameter(
         new Name("MaxSv", "Max Sv"),
         Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Unit.DB,
         "Maximum Sv in school");

   public final RangeParameter length = new RangeParameter(
         new Name("Length"),
         25, 1000000, Unit.METER,
         "Length of school");

   public final RangeParameter thickness = new RangeParameter(
         new Name("Thickness"),
         10, 1000000, Unit.METER,
         "Thickness of school");

   public final RangeParameter area = new RangeParameter(
         new Name("Area"),
         75, 1000000, Unit.METER_2,
         "Area of school");

   public final RangeParameter compactness = new RangeParameter(
         new Name("Compactness"),
         Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Unit.DIMENSIONLESS,
         "Perimeter of circle with same area / Perimeter");

   private final HeaderParameter postprocessingHeader = new HeaderParameter("Postprocessing");

   public final BooleanParameter fillHoles = new BooleanParameter(
         new Name("FillHoles", "Fill holes"),
         true,
         "If selected, then holes in detected schools will be filled");

   public SchoolDetectionModule() {
      processLast.addListenerAndNotify(last -> {
         channel.setEnabled(!last);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            detectionHeader,
            processLast,
            channel,
            minDepth,
            maxDepth,
            threshold,
            //---
            acceptanceHeader,
            meanSv,
            maxSv,
            length,
            thickness,
            area,
            compactness,
            //---
            postprocessingHeader,
            fillHoles
      );
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(TransducerRangesFileService.NAME);
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new SchoolDetectionModuleComputation(this, computationContext, pingSource);
   }
}
