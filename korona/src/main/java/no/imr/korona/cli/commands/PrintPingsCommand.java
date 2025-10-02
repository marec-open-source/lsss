package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class PrintPingsCommand extends CliCommand {
   public static final String COMMAND_NAME = "print-pings";
   public static final String DESCRIPTION = "Prints pings (experimental)";

   private final OptionSpec<Path> files = parser.nonOptions("Files or directories")
         .withValuesConvertedBy(new AbsolutePathConverter());

   public PrintPingsCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new PrintPingsCommandJob(
            options.valuesOf(files)
      );
   }
}
