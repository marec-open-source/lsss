package no.imr.korona.util.simulator;

import no.imr.korona.data.DataFormatManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.formats.ek60.EK60DataFormatPlugin;
import no.imr.korona.data.formats.ek60.EK60Writer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.util.PingReaderFactory;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.imr.tools.time.NTDate;
import no.imr.tools.time.RealtimeSyncer;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Simulates an echosounder by writing data files to a directory.
 */
public final class EchoSounderSimulator implements ParameterContainer {
   private static final String FILE_NAME_PREFIX = "EchoSounderSimulator_";

   public final FileParameter input = new FileParameter(
         new Name("Input"),
         null, FileParameter.Mode.FILE_OR_DIRECTORY);

   public final FileParameter output = new FileParameter(
         new Name("Output"),
         null, FileParameter.Mode.DIRECTORY);

   public final FloatParameter realtimeFactor = new FloatParameter(
         new Name("RealtimeFactor", "Realtime factor"),
         1, Unit.DIMENSIONLESS, ValueConstraints.gte(0f));

   public final OptionalIntParameter pingsPerFile = new OptionalIntParameter(
         new Name("PingsPerFile", "Pings per file"),
         Optional.empty(), Unit.COUNT, ValueConstraints.gte(1));

   public final BooleanParameter repeat = new BooleanParameter(
         new Name("Repeat"),
         false);

   public final BooleanParameter writeOnlyRaw = new BooleanParameter(
         new Name("OnlyRaw", "Only raw"),
         false);

   public final BooleanParameter writeCurrentTime = new BooleanParameter(
         new Name("WriteCurrentTime", "Write current time"),
         true);

   public final BooleanParameter deleteOldFiles = new BooleanParameter(
         new Name("DeleteOldFiles", "Delete old files"),
         false);

   public final IntParameter oldFilesToKeep = new IntParameter(
         new Name("OldFilesToKeep", "Old files to keep"),
         5, Unit.COUNT);

   private final ButtonParameter newFile = new ButtonParameter(
         new Name("NewFile", "New file"),
         "",
         () -> forceNewFile = true);

   private final DataFormatManager dataFormatManager = new DataFormatManager();

   private final DateTimeFormatter dateFormat = Utils.createUTCDateTimeFormatter("yyyy-MM-dd_HHmmss");

   private final ChangeManager changeManager = new ChangeManager();
   private final Executor executor = Executors.newSingleThreadExecutor(Exec.newThreadFactory("EchoSounderSimulator"));
   private AsyncHandle asyncHandle = new AsyncHandle();
   private long pingNumber;
   private int pingCounter;
   private @Nullable Path currentOutputFile;
   private int fileCounter;
   private long lastTime = System.currentTimeMillis();
   private boolean forceNewFile;
   private @Nullable PingReader pingReader;

   private boolean running;
   private final ArgChangeManager<Boolean> runningChangeManager = new ArgChangeManager<>();

   public EchoSounderSimulator() {
      deleteOldFiles.addListenerAndNotify(oldFilesToKeep::setEnabled);

      Listener listener = () -> {
         if (running) {
            setRunning(false);
            setRunning(true);
         }
      };
      listener.addTo(
            input,
            output
      );
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            input,
            output,
            realtimeFactor,
            pingsPerFile,
            repeat,
            writeOnlyRaw,
            writeCurrentTime,
            deleteOldFiles,
            oldFilesToKeep,
            newFile
      );
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public long getPingNumber() {
      return pingNumber;
   }

   public int getPingCounter() {
      return pingCounter;
   }

   public int getFileCounter() {
      return fileCounter;
   }

   public @Nullable Path getCurrentOutputFile() {
      return currentOutputFile;
   }

   public @Nullable Path getCurrentInputFile() {
      return pingReader != null ? pingReader.getFile() : null;
   }

   public Element toXml() {
      Element element = DocumentHelper.createElement("EchoSounderSimulator");
      element.add(new ParameterCollection(this).toXml());
      return element;
   }

   public void fromXml(Element element) {
      Element parametersElement = element.element(ParameterCollection.XML_PARAMETERS);
      if (parametersElement != null) {
         new ParameterCollection(this).fromXml(parametersElement);
      }
   }

   private void start() {
      executor.execute(asyncHandle.createManagedRunnable(() -> {
         try {
            run();
         } finally {
            executor.execute(() -> setRunning(false));
         }
      }));
   }

   private void stop() {
      asyncHandle.cancel();
      asyncHandle.waitUntilFinished();
      asyncHandle = new AsyncHandle();
   }

   public boolean isRunning() {
      return running;
   }

   public void setRunning(boolean running) {
      if (this.running == running) {
         return;
      }
      this.running = running;
      if (running) {
         start();
      } else {
         stop();
      }
      runningChangeManager.notifyListeners(running);
   }

   public ArgChangeManager<Boolean> getRunningChangeManager() {
      return runningChangeManager;
   }

   private String getFileName(long ntDate) {
      return FILE_NAME_PREFIX + dateFormat.format(Instant.ofEpochMilli(NTDate.ntDateToTimeInMillis(ntDate))) + EK60DataFormatPlugin.RAW_SUFFIX;
   }

   private void tryDeleteOldFiles() {
      try {
         doDeleteOldFiles();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
   }

   private void doDeleteOldFiles() throws IOException {
      Path outputDir = output.getFile();
      if (outputDir == null) {
         return;
      }
      List<Path> files = FileUtils.listFiles(outputDir, file -> file.getFileName().toString().startsWith(FILE_NAME_PREFIX));
      int oldFileCount = oldFilesToKeep.getIntValue();
      if (!writeOnlyRaw.getBooleanValue()) {
         oldFileCount *= 3;
      }
      if (files.size() <= oldFileCount) {
         return;
      }
      files.sort(null);
      for (Path file : files.subList(0, files.size() - oldFileCount)) {
         Files.deleteIfExists(file);
      }
   }

   private void run() {
      boolean shouldLogError = true;
      while (!asyncHandle.isCancelled()) {
         Path inputFile = input.getFile();
         if (inputFile == null) {
            return;
         }
         try (PingReader newPingReader = PingReaderFactory.create(dataFormatManager, inputFile, true)) {
            pingReader = newPingReader;
            try (FileSplitter fileSplitter = new FileSplitter(this, newPingReader)) {
               while (!asyncHandle.isCancelled()) {
                  fileSplitter.nextPing();
               }
            }
            shouldLogError = true;
         } catch (IOException e) {
            if (shouldLogError) {
               Log.global.log(Level.WARNING, "Error writing " + inputFile + " to " + output.getFile(), e);
               shouldLogError = false;
            }
            asyncHandle.sleep(1000);
         } finally {
            pingReader = null;
         }

         if (!repeat.getBooleanValue()) {
            break;
         }
      }
   }

   private static final class FileSplitter implements AutoCloseable {
      private final EchoSounderSimulator echoSounderSimulator;
      private final PingSource pingSource;
      private final PingConfiguration pingConfiguration;
      private final RealtimeSyncer realtimeSyncer = new RealtimeSyncer();
      private @Nullable EK60Writer ek60Writer;

      private FileSplitter(EchoSounderSimulator echoSounderSimulator, PingSource pingSource) throws IOException {
         this.echoSounderSimulator = echoSounderSimulator;
         this.pingSource = pingSource;

         pingConfiguration = pingSource.getPingConfiguration();
         if (echoSounderSimulator.writeCurrentTime.getBooleanValue()) {
            long ntDate = getCurrentNTDate();
            setNTDate(ntDate, pingConfiguration.getConfigurationItems());
         }
         reopen(getCurrentNTDate());
      }

      @Override
      public void close() throws IOException {
         if (ek60Writer != null) {
            ek60Writer.close();
         }
      }

      private void nextPing() throws IOException {
         Ping ping = pingSource.nextPing(echoSounderSimulator.asyncHandle);
         if (ping == null || echoSounderSimulator.asyncHandle.isCancelled()) {
            return;
         }

         realtimeSyncer.sync(ping.getTimeInMillis(), echoSounderSimulator.realtimeFactor.getFloatValue(), false);

         long ntDate = getCurrentNTDate();

         if (echoSounderSimulator.forceNewFile || echoSounderSimulator.pingCounter >= echoSounderSimulator.pingsPerFile.getValue().orElse(Integer.MAX_VALUE)) {
            reopen(ntDate);
         }

         if (echoSounderSimulator.writeCurrentTime.getBooleanValue()) {
            ((Idx0Datagram) ping.getPingIndex()).setNTDate(ntDate);
            ping.getBot0Datagram().setNTDate(ntDate);
            setNTDate(ntDate, ping.getPingItems());
         }
         ping.getPingIndex().setPingNumber(echoSounderSimulator.pingNumber);

         if (ek60Writer != null) {
            ek60Writer.write(ping);
         }
         echoSounderSimulator.pingCounter++;
         echoSounderSimulator.pingNumber++;
         echoSounderSimulator.changeManager.notifyListeners();
      }

      private void reopen(long ntDate) throws IOException {
         echoSounderSimulator.pingCounter = 0;
         echoSounderSimulator.forceNewFile = false;
         String fileName = echoSounderSimulator.getFileName(ntDate);
         Path outputDir = echoSounderSimulator.output.getFile();
         if (outputDir == null) {
            return;
         }
         echoSounderSimulator.currentOutputFile = outputDir.resolve(fileName);
         echoSounderSimulator.fileCounter++;

         if (ek60Writer != null) {
            ek60Writer.close();
         }

         if (echoSounderSimulator.deleteOldFiles.getBooleanValue()) {
            echoSounderSimulator.tryDeleteOldFiles();
         }

         if (echoSounderSimulator.writeCurrentTime.getBooleanValue()) {
            setNTDate(ntDate, pingConfiguration.getConfigurationItems());
         }
         boolean onlyRaw = echoSounderSimulator.writeOnlyRaw.getBooleanValue();
         EK60Writer.Mode writerMode = onlyRaw ? EK60Writer.Mode.ONLY_RAW : EK60Writer.Mode.ALL_FILES;
         ek60Writer = new EK60Writer(outputDir, fileName, pingConfiguration, "", writerMode);
      }

      private long getCurrentNTDate() {
         while (true) { // Make sure next ping has a different time in millis.
            long time = System.currentTimeMillis();
            if (time != echoSounderSimulator.lastTime) {
               echoSounderSimulator.lastTime = time;
               return NTDate.timeInMillisToNTDate(time);
            }
            Utils.sleep(1);
         }
      }

      private static void setNTDate(long ntDate, List<PingItem> pingItems) {
         for (PingItem pingItem : pingItems) {
            pingItem.setNTDate(ntDate);
         }
      }
   }
}
