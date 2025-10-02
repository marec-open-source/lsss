package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class RawToJsonCommand extends CliCommand {
   public static final String COMMAND_NAME = "raw-to-json";
   public static final String DESCRIPTION = "Prints datagrams as JSON (experimental)";

   private final OptionSpec<Path> filesSpec = parser.nonOptions("Files or directories")
         .withValuesConvertedBy(new AbsolutePathConverter());

   public RawToJsonCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new RawToJsonCommandJob(
            options.valuesOf(filesSpec)
      );
   }
}
