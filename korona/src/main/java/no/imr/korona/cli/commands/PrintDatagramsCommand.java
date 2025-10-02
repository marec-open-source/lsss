package no.imr.korona.cli.commands;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import no.imr.korona.cli.AbsolutePathConverter;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandJob;

import java.nio.file.Path;
import java.util.Set;

public final class PrintDatagramsCommand extends CliCommand {
   public static final String COMMAND_NAME = "print-datagrams";
   public static final String DESCRIPTION = "Prints info about the datagrams in one or more files or directories";

   private final OptionSpec<Long> limit = parser.accepts("limit", "Limits the number of datagrams to print")
         .withRequiredArg()
         .ofType(Long.class);

   private final OptionSpec<String> only = parser.accepts("only", "Comma-separated list of datagram types to print")
         .withRequiredArg()
         .withValuesSeparatedBy(',');

   private final OptionSpec<String> skip = parser.accepts("skip", "Comma-separated list of datagram types to not print")
         .withRequiredArg()
         .withValuesSeparatedBy(',');

   private final OptionSpec<Path> files = parser.nonOptions("Files or directories")
         .withValuesConvertedBy(new AbsolutePathConverter());

   public PrintDatagramsCommand() {
      super(DESCRIPTION);
   }

   @Override
   public CliCommandJob createJob(OptionSet options) {
      return new PrintDatagramsCommandJob(
            options.has(limit) ? options.valueOf(limit) : Long.MAX_VALUE,
            Set.copyOf(options.valuesOf(only)),
            Set.copyOf(options.valuesOf(skip)),
            options.valuesOf(files)
      );
   }
}
