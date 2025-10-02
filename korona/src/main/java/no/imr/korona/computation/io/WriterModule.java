package no.imr.korona.computation.io;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.formats.ek60.EK60Writer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.input.GUIConfig;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class WriterModule extends SimplePingModule {
   public final StringParameter fileName = new StringParameter(
         new Name("FileName", "File name"),
         "",
         "Output file name. If blank, the input file name is used.");

   public final BooleanParameter useRelativeDirectory = new BooleanParameter(
         new Name("UseRelativeDirectory", "Use relative directory"),
         false,
         "Whether to specify a relative or an absolute output directory");

   public final StringParameter relativeDirectory = new StringParameter(
         new Name("RelativeDirectory", "Relative directory"),
         "",
         "Output directory relative to the destination directory");

   public final FileParameter directory = new FileParameter(
         new Name("DirectoryName", "Directory"),
         null, FileParameter.Mode.DIRECTORY,
         "Output directory");

   private String extraSuffix = "";

   public WriterModule() {
      useRelativeDirectory.addListenerAndNotify(useRelative -> {
         relativeDirectory.setVisible(useRelative);
         relativeDirectory.setPersistable(useRelative);
         directory.setVisible(!useRelative);
         directory.setPersistable(!useRelative);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            fileName,
            useRelativeDirectory,
            relativeDirectory,
            directory
      );
   }

   @Override
   public void customizeGUIConfig(GUIConfig guiConfig) {
      guiConfig.setHorizontalFill(true);
      guiConfig.setCombineInputAndDescription(true);
      guiConfig.setTextAlignment(GUIConfig.Alignment.LEFT);
   }

   public void setExtraSuffix(String suffix) {
      extraSuffix = suffix;
   }

   @Override
   public @Nullable SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      if (computationContext.isUsingKoronaPlaybox()) {
         return null;
      }
      Path dir;
      if (useRelativeDirectory.getBooleanValue()) {
         Path koronaDirectory = computationContext.getAssociatedKoronaDirectory();
         if (koronaDirectory == null) {
            throw new ModuleConfigurationException(this, "Using relative path, but no destination directory configured");
         }
         dir = koronaDirectory.resolve(FileUtils.toNativeSeparatorChar(relativeDirectory.getValue())).normalize();
      } else {
         dir = directory.getFile();
         if (dir == null) {
            throw new ModuleConfigurationException(this, "No output directory");
         }
      }
      return new WriterModuleComputation(this, computationContext, pingSource, dir);
   }

   private static final class WriterModuleComputation extends SimplePingModuleComputation {
      private final EK60Writer ek60Writer;

      private WriterModuleComputation(WriterModule module, ComputationContext computationContext, PingSource pingSource, Path dir) throws IOException {
         super(module, computationContext, pingSource);

         String fileName = module.fileName.getValue().trim();
         if (fileName.isEmpty()) {
            fileName = computationContext.getPingReader().getFile().getFileName().toString();
         }
         ek60Writer = new EK60Writer(dir, fileName, getPingConfiguration(), module.extraSuffix, EK60Writer.Mode.ALL_FILES);
         ek60Writer.writeModuleConfiguration(getPingConfiguration(), computationContext.getModuleContainer());
      }

      @Override
      protected void processPing(Ping ping) throws IOException {
         ek60Writer.write(ping);
      }

      @Override
      public void close() throws IOException {
         ek60Writer.close();
      }
   }
}
