package no.imr.korona.cli;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import no.imr.korona.Korona;
import no.imr.korona.resources.KoronaResource;
import no.imr.tools.Utils;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.logging.LoggingManager;

import java.io.IOException;
import java.util.Arrays;

@SuppressWarnings("PMD.SystemPrintln")
public final class KoronaCli {
   public static final ApplicationInfo APPLICATION_INFO = new ApplicationInfo(
         "LSSS", "Large Scale Survey System", "KoronaCli", Korona.VERSION,
         KoronaResource.KORONA_32, KoronaResource.KORONA_64,
         "korona", "lsss");
   public static final LoggingManager LOGGING_MANAGER = new LoggingManager(APPLICATION_INFO);

   private KoronaCli() {
   }

   public static void main(String[] args) {
      Utils.init(args);
      if (Utils.isTestRun()) {
         KoronaCliSmoke.main(args);
         return;
      }

      LOGGING_MANAGER.setUseWindowHandler(false);
      LOGGING_MANAGER.startLogging();

      int exitCode = run(args);

      LOGGING_MANAGER.shutDown(exitCode);
   }

   public static int run(String[] args) {
      if (args.length == 0) {
         printUsage();
         return 1;
      }

      String commandName = args[0];
      if (commandName.equals("--help") || commandName.equals("-h")) {
         printUsage();
         return 0;
      }

      CliCommand command = CliCommandFactory.create(commandName);
      if (command == null) {
         System.err.println();
         System.err.println("Unrecognized command: " + commandName);
         printUsage();
         return 1;
      }

      CliCommandJob job;
      try {
         OptionSet options = command.parser.parse(Arrays.copyOfRange(args, 1, args.length));
         if (options.has(command.help)) {
            printHelp(commandName + ": " + command.description, command.parser);
            return 0;
         }
         job = command.createJob(options);
      } catch (Exception e) {
         printHelp("Error parsing options: " + e.getMessage(), command.parser);
         return 1;
      }

      try {
         job.run(System.in, System.out);
      } catch (Exception e) {
         System.err.println();
         System.err.println("Error executing command: " + commandName);
         System.err.println();
         e.printStackTrace(System.err);
         return 1;
      }

      return 0;
   }

   private static void printUsage() {
      System.err.println();
      System.err.println("Usage: KoronaCli <command> [options and arguments]");
      System.err.println();
      System.err.println("Available commands:");
      for (CliCommandInfo info : CliCommandFactory.infos()) {
         System.err.println("  " + info.name() + ": " + info.description());
      }
      System.err.println();
      System.err.println("For help on a specific command, run `KoronaCli <command> --help`");
      System.err.println();
   }

   private static void printHelp(String message, OptionParser parser) {
      System.err.println();
      System.err.println(message);
      System.err.println();
      try {
         parser.printHelpOn(System.err);
      } catch (IOException e) {
         System.err.println("Error printing help");
         e.printStackTrace(System.err);
      }
      System.err.println();
   }
}
