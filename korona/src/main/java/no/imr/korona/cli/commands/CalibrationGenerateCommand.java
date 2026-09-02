package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class CalibrationGenerateCommand extends CliCommand {
   public static final String COMMAND_NAME = "calibration-generate";
   public static final String DESCRIPTION = "Generates calibration.xml with values from data files";

   private final OptionSpec<Path> dir = parser.accepts("dir", "Directory with data files")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> output = parser.accepts("output", "Output file as an absolute path or relative to dir. If not specified, then stdout is used")
         .withRequiredArg()
         .withValuesConvertedBy(new PathConverter());

   public CalibrationGenerateCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new CalibrationGenerateCommandJob(
            options.valueOf(dir),
            options.valueOf(output)
      );
   }
}
