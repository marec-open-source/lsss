package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class BatchCommand extends CliCommand {
   public static final String COMMAND_NAME = "batch";
   public static final String DESCRIPTION = "Processes the files in the source directory";

   private final OptionSpec<Path> cfs = parser.accepts("cfs", "Config file settings, including module setup")
         .withRequiredArg()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> source = parser.accepts("source", "Directory with files to be processed")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> destination = parser.accepts("destination", "Directory where the processed files will be written")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Integer> maxParallel = parser.accepts("max-parallel",
               "Maximum parallel processes, defaulting to number of available processors")
         .withRequiredArg()
         .ofType(Integer.class)
         .defaultsTo(Runtime.getRuntime().availableProcessors());

   public BatchCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new BatchCommandJob(
            options.valueOf(cfs),
            options.valueOf(source),
            options.valueOf(destination),
            options.valueOf(maxParallel)
      );
   }
}
