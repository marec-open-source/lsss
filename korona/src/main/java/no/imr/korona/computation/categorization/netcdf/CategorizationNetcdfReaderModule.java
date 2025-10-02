package no.imr.korona.computation.categorization.netcdf;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.IgnoreModuleComputationException;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class CategorizationNetcdfReaderModule extends ConcurrentPingModule {
   public final FileParameter inputFile = new FileParameter(
         new Name("InputFile", "Input file"),
         null, FileParameter.Mode.FILE,
         "The netCDF data is read from this file");

   public final OptionalIntParameter mainFrequency = new OptionalIntParameter(
         new Name("MainFrequency", "Main frequency"),
         Optional.of(38), Unit.KHZ, ValueConstraints.gt(0),
         "The channel with the main frequency is used for converting range to depth");

   public CategorizationNetcdfReaderModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            inputFile,
            mainFrequency
      );
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException {
      return new CategorizationNetcdfReaderModuleComputation(this, computationContext, pingSource);
   }
}
