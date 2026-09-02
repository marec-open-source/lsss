package no.imr.korona.computation.categorization.netcdf;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.IntCsvListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class CategorizationNetcdfWriterModule extends SimplePingModule {
   public final StringParameter dirName = new StringParameter(
         new Name("DirName", "Output directory name"),
         "categorization",
         "The netCDF files are written to this subfolder in the destination directory");

   public final IntCsvListParameter mainFrequency = new IntCsvListParameter(
         new Name("MainFrequency", "Main frequency"),
         List.of(38), Unit.KHZ, ValueConstraints.gt(0),
         """
               A comma-separated list of prioritized candidates for the main frequency.
               If unspecified, the first channel is used.
               The channel with the main frequency is used for converting depth to range""");

   public final OptionalFloatParameter deltaRange = new OptionalFloatParameter(
         new Name("DeltaRange", "Delta range (range resolution)"),
         Optional.empty(), Unit.METER, ValueConstraints.gt(0f),
         "Leave blank to determine automatically");

   public final OptionalFloatParameter maxRange = new OptionalFloatParameter(
         new Name("MaxRange", "Max range from transducer"),
         Optional.empty(), Unit.METER, ValueConstraints.gt(0f),
         "Leave blank to determine automatically");

   public CategorizationNetcdfWriterModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            dirName,
            mainFrequency,
            deltaRange,
            maxRange
      );
   }

   @Override
   public @Nullable SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      if (computationContext.isUsingKoronaPlaybox()) {
         return null;
      }
      return new CategorizationNetcdfWriterModuleComputation(this, computationContext, pingSource);
   }
}
