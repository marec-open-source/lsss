package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class WorkFileToNetcdfCommand extends CliCommand {
   public static final String COMMAND_NAME = "work-file-to-netcdf";
   public static final String DESCRIPTION = "Converts work files to NetCDF format (beta)";

   private final OptionSpec<Path> dataDir = parser.accepts("data-dir", "Directory with data files")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> workDir = parser.accepts("work-dir", "Directory with work files")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> outputDir = parser.accepts("output-dir", "Directory where to write work files as NetCDF")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Integer> maxParallel = parser.accepts("max-parallel",
               "Maximum parallel processes, defaulting to the number of available processors")
         .withRequiredArg()
         .ofType(Integer.class)
         .defaultsTo(Runtime.getRuntime().availableProcessors());

   private final OptionSpec<Integer> frequency = parser.accepts("frequency",
               "The frequency [kHz] to use")
         .withRequiredArg()
         .ofType(Integer.class)
         .defaultsTo(38);

   private final OptionSpec<Float> deltaRange = parser.accepts("delta-range",
               "The range resolution [m]")
         .withRequiredArg()
         .required()
         .ofType(Float.class);

   private final OptionSpec<Float> maxRange = parser.accepts("max-range",
               "The maximum range [m]")
         .withRequiredArg()
         .required()
         .ofType(Float.class);

   public WorkFileToNetcdfCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new WorkDirProcessor(
            options.valueOf(dataDir),
            options.valueOf(workDir),
            options.valueOf(outputDir),
            options.valueOf(maxParallel),
            new WorkFileNetcdfWriter(
                  options.valueOf(outputDir),
                  options.valueOf(frequency),
                  options.valueOf(deltaRange),
                  options.valueOf(maxRange)
            )
      );
   }
}
