package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class ExtractMetadataCommand extends CliCommand {
   public static final String COMMAND_NAME = "extract-metadata";
   public static final String DESCRIPTION = "Extracts metadata for the specified data files";

   private final OptionSpec<Path> dir = parser.accepts("dir", "Directory with data files")
         .withRequiredArg()
         .required()
         .withValuesConvertedBy(new AbsolutePathConverter());

   private final OptionSpec<Path> output = parser.accepts("output", "Output file as an absolute path or relative to dir. If not specified, then stdout is used")
         .withRequiredArg()
         .withValuesConvertedBy(new PathConverter());

   public ExtractMetadataCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new ExtractMetadataCommandJob(
            options.valueOf(dir),
            options.valueOf(output)
      );
   }
}
