package no.imr.korona.cli;

import no.imr.korona.cli.commands.BatchCommand;
import no.imr.korona.cli.commands.CalibrationGenerateCommand;
import no.imr.korona.cli.commands.EchosounderSimulatorCommand;
import no.imr.korona.cli.commands.ExtractMetadataCommand;
import no.imr.korona.cli.commands.FixIdxCommand;
import no.imr.korona.cli.commands.KoronaInfoCommand;
import no.imr.korona.cli.commands.PrintDatagramsCommand;
import no.imr.korona.cli.commands.PrintPingsCommand;
import no.imr.korona.cli.commands.RawToJsonCommand;
import no.imr.korona.cli.commands.WorkFileToNetcdfCommand;
import no.imr.korona.cli.commands.WorkFileUpgradeCommand;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class CliCommandFactory {
   private CliCommandFactory() {
   }

   public static List<CliCommandInfo> infos() {
      return List.of(
            new CliCommandInfo(BatchCommand.COMMAND_NAME, BatchCommand.DESCRIPTION),
            new CliCommandInfo(CalibrationGenerateCommand.COMMAND_NAME, CalibrationGenerateCommand.DESCRIPTION),
            // experimental: new CliCommandInfo(EchosounderSimulatorCommand.COMMAND_NAME, EchosounderSimulatorCommand.DESCRIPTION),
            // experimental: new CliCommandInfo(MetadataCommand.COMMAND_NAME, MetadataCommand.DESCRIPTION),
            // experimental: new CliCommandInfo(FixIdxCommand.COMMAND_NAME, FixIdxCommand.DESCRIPTION),
            new CliCommandInfo(KoronaInfoCommand.COMMAND_NAME, KoronaInfoCommand.DESCRIPTION),
            new CliCommandInfo(PrintDatagramsCommand.COMMAND_NAME, PrintDatagramsCommand.DESCRIPTION),
            // experimental: new CliCommandInfo(PrintPingsCommand.COMMAND_NAME, PrintPingsCommand.DESCRIPTION),
            // experimental: new CliCommandInfo(RawToJsonCommand.COMMAND_NAME, RawToJsonCommand.DESCRIPTION),
            new CliCommandInfo(WorkFileToNetcdfCommand.COMMAND_NAME, WorkFileToNetcdfCommand.DESCRIPTION)
            // experimental: new CliCommandInfo(WorkFileUpgradeCommand.COMMAND_NAME, WorkFileUpgradeCommand.DESCRIPTION),
      );
   }

   public static @Nullable CliCommand create(String commandName) {
      return switch (commandName) {
         case BatchCommand.COMMAND_NAME -> new BatchCommand();
         case CalibrationGenerateCommand.COMMAND_NAME -> new CalibrationGenerateCommand();
         case EchosounderSimulatorCommand.COMMAND_NAME -> new EchosounderSimulatorCommand();
         case ExtractMetadataCommand.COMMAND_NAME -> new ExtractMetadataCommand();
         case FixIdxCommand.COMMAND_NAME -> new FixIdxCommand();
         case KoronaInfoCommand.COMMAND_NAME -> new KoronaInfoCommand();
         case PrintDatagramsCommand.COMMAND_NAME -> new PrintDatagramsCommand();
         case PrintPingsCommand.COMMAND_NAME -> new PrintPingsCommand();
         case RawToJsonCommand.COMMAND_NAME -> new RawToJsonCommand();
         case WorkFileToNetcdfCommand.COMMAND_NAME -> new WorkFileToNetcdfCommand();
         case WorkFileUpgradeCommand.COMMAND_NAME -> new WorkFileUpgradeCommand();
         default -> null;
      };
   }
}
