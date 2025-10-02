package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

public final class KoronaInfoCommand extends CliCommand {
   public static final String COMMAND_NAME = "korona-info";
   public static final String DESCRIPTION = "Prints info about KORONA, including modules and parameters, in JSON format";

   public KoronaInfoCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new KoronaInfoCommandJob();
   }
}
