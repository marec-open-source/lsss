package no.imr.korona.cli;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public abstract class CliCommand {
   public final OptionParser parser = new OptionParser();
   public final OptionSpec<Void> help = parser.accepts("help", "Displays help information").forHelp();
   public final String description;

   protected CliCommand(String description) {
      this.description = description;
   }

   public abstract CliCommandJob createJob(OptionSet options);
}
