package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class FixIdxCommand extends CliCommand {
   public static final String COMMAND_NAME = "fix-idx";
   public static final String DESCRIPTION = "Creates a new set of idx-files, with adjusted time, vessel distance and ping number";

   private final OptionSpec<Path> source = parser.accepts("source", "Directory with the original idx and raw files")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> destination = parser.accepts("destination",
               "Directory where the adjusted idx-files will be written")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   public FixIdxCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new FixIdxCommandJob(
            options.valueOf(source),
            options.valueOf(destination)
      );
   }
}
