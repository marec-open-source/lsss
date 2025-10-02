package no.imr.korona.util.realtime.app;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.util.realtime.ProcessingStatus;
import no.imr.korona.util.realtime.RealtimeProcessor;
import no.imr.tools.io.DirectoryWatcher;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

public final class RealtimeProcessorApp {
   private final Path configFile;
   private final ProcessorConfig processorConfig;
   private final Korona korona;
   private final ProcessingStatus processingStatus = new ProcessingStatus();
   private final @Nullable DirectoryWatcher directoryWatcher;
   private @Nullable RealtimeProcessor realtimeProcessor;

   public RealtimeProcessorApp(Path configFile, Korona korona) {
      this.configFile = configFile;
      this.korona = korona;
      processorConfig = new ProcessorConfig(korona);

      try {
         FileUtils.createDirectories(configFile.getParent());
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error creating " + configFile.getParent(), e);
      }

      loadConfig();
      directoryWatcher = DirectoryWatcher.forFile(configFile, this::loadConfig);
   }

   public Path getConfigFile() {
      return configFile;
   }

   public Korona getKorona() {
      return korona;
   }

   public ProcessingStatus getProcessingStatus() {
      return processingStatus;
   }

   private void loadConfig() {
      try {
         Log.global.info("Loading " + configFile);
         processorConfig.load(configFile);
         startProcessing();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error loading " + configFile, e);
      }
   }

   private void startProcessing() {
      stop();

      Path sourceDirectory = processorConfig.sourceDirectory.getFile();
      Path destinationDirectory = processorConfig.destinationDirectory.getFile();
      if (sourceDirectory == null || destinationDirectory == null) {
         return;
      }

      ModuleContainer moduleContainer;
      try {
         moduleContainer = processorConfig.getCfsManager().loadModuleContainer();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error starting processing", e);
         return;
      }

      realtimeProcessor = new RealtimeProcessor(moduleContainer, processingStatus, sourceDirectory)
            .setDestinationDirectory(destinationDirectory)
            .setSkipToEndOfFirstFile(true)
            .setOnlyRaw(processorConfig.onlyRaw.getBooleanValue())
            .start();
   }

   private void stop() {
      if (realtimeProcessor != null) {
         realtimeProcessor.close();
      }
      realtimeProcessor = null;
   }

   public void close() {
      stop();
      if (directoryWatcher != null) {
         directoryWatcher.close();
      }
   }
}
