package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class WorkFileUpgradeCommand extends CliCommand {
   public static final String COMMAND_NAME = "work-file-upgrade";
   public static final String DESCRIPTION = "Upgrades work files to the newest version";

   private final OptionSpec<Path> dataDir = parser.accepts("data-dir", "Directory with data files")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> workDir = parser.accepts("work-dir", "Directory with work files")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> outputDir = parser.accepts("output-dir", "Directory where to write upgraded work files")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Integer> maxParallel = parser.accepts("max-parallel",
               "Maximum parallel processes, defaulting to number of available processors")
         .withRequiredArg()
         .ofType(Integer.class)
         .defaultsTo(Runtime.getRuntime().availableProcessors());

   public WorkFileUpgradeCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new WorkDirProcessor(
            options.valueOf(dataDir),
            options.valueOf(workDir),
            options.valueOf(outputDir),
            options.valueOf(maxParallel),
            new WorkFileXmlWriter(options.valueOf(outputDir))
      );
   }
}
