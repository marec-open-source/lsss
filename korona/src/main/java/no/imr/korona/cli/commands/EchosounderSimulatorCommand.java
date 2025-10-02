package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;

public final class EchosounderSimulatorCommand extends CliCommand {
   public static final String COMMAND_NAME = "echosounder-simulator";
   public static final String DESCRIPTION = "Simulates an echosounder by writing existing files to a new directory";

   private final OptionSpec<?> gui = parser.accepts("gui", "Shows the echosounder simulator GUI");

   private final OptionSpec<Path> cfg = parser.accepts("cfg", "Echosounder simulator config file")
         .withRequiredArg()
         .withValuesConvertedBy(new AbsolutePathConverter());

   public EchosounderSimulatorCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new EchosounderSimulatorCommandJob(
            options.has(gui),
            options.has(cfg) ? options.valueOf(cfg) : null
      );
   }
}
