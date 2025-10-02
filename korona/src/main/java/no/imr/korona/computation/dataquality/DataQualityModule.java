package no.imr.korona.computation.dataquality;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class DataQualityModule extends SimplePingModule {
   public final StringParameter dirName = new StringParameter(
         new Name("DirName", "Output directory name"),
         "dataQuality",
         "The netCDF files are written to this subfolder in the destination directory");

   public final OptionalIntParameter mainFrequency = new OptionalIntParameter(
         new Name("MainFrequency", "Main frequency"),
         Optional.of(38), Unit.KHZ, ValueConstraints.gt(0),
         """
               The channel with the main frequency is used for determining the bottom depth.
               If unspecified, the first channel is used""");

   public DataQualityModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            dirName,
            mainFrequency
      );
   }

   @Override
   public @Nullable SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      if (computationContext.isUsingKoronaPlaybox()) {
         return null;
      }
      return new DataQualityModuleComputation(this, computationContext, pingSource);
   }
}
