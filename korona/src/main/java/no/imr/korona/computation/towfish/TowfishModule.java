package no.imr.korona.computation.towfish;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.IgnoreModuleComputationException;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Merges towfish data with main echosounder data.
 */
public final class TowfishModule extends SimplePingModule {
   private static final String CONSYS_DATA_DIR = "Consys-metadata";
   private static final String MESSOR_DATA_DIR = "Messor-metadata";
   private static final String MUST_DATA_DIR = "Must-metadata";

   public final FloatParameter transducerBlindZone = new FloatParameter(
         new Name("TowfishBlindZone", "Towfish blind zone"),
         0, Unit.METER, ValueConstraints.gte(0f),
         "The length of the towfish blind zone. The samples in the blind zone are excluded when merging the towfish data with input data");

   public final FloatParameter towfishEchosounderTimeAddition = new FloatParameter(
         new Name("TowfishEchosounderTimeAddition", "Time to add to the towfish echosounder data"),
         0, Unit.SECONDS,
         "Time to add to the towfish echosounder data to get at match with the meta data and ship data");

   public final ObjectParameter<TowfishMetaData.DistBehindFunction> distBehindFunction = new ObjectParameter<>(
         new Name("DistBehindFunction", "Distance behind function"),
         TowfishMetaData.DistBehindFunction.PYTAGORAS, TowfishMetaData.DistBehindFunction.values(),
         "Relationship between line length and depth to towfish distance behind ship");

   public TowfishModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            transducerBlindZone,
            towfishEchosounderTimeAddition,
            distBehindFunction
      );
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(TowfishFileService.NAME);
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException {
      return new TowfishModuleComputation(this, computationContext, pingSource);
   }

   private static final class TowfishModuleComputation extends SimplePingModuleComputation {
      private final TowfishModule module;

      private @Nullable PingReader towfishPingReader;
      private final TowfishMetaData towfishMetaData;
      private @Nullable Ping lastTowfishPing;
      private @Nullable Ping beforeLastTowfishPing;

      private @Nullable PingIndex lastUsedTowfishPingIndex;

      private final Duration towFishEchosounderAddition;
      private final NavigableMap<Instant, SegmentHandle> towfishSegmentHandles = new TreeMap<>();
      private Instant towfishHandleTime = Instant.EPOCH;

      private TowfishModuleComputation(TowfishModule module, ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException {
         super(module, computationContext, pingSource);

         this.module = module;
         towFishEchosounderAddition = Duration.ofMillis((long) (module.towfishEchosounderTimeAddition.getFloatValue() * 1000));

         Path towfishDir = module.getRequiredConfigFile(TowfishFileService.NAME);
         if (!Files.exists(towfishDir)) {
            throw new IgnoreModuleComputationException();
         }

         Path consysDataDir = towfishDir.resolve(CONSYS_DATA_DIR);
         Path messorDataDir = towfishDir.resolve(MESSOR_DATA_DIR);
         Path mustDataDir = towfishDir.resolve(MUST_DATA_DIR);

         if (Files.exists(consysDataDir)) {
            towfishMetaData = new TowfishMetaData(new ConsysFileReader(), FileUtils.listFiles(consysDataDir));
         } else if (Files.exists(messorDataDir)) {
            towfishMetaData = new TowfishMetaData(new MessorFileReader(), FileUtils.listFiles(messorDataDir));
         } else if (Files.exists(mustDataDir)) {
            towfishMetaData = new TowfishMetaData(new MustFileReader(), FileUtils.listFiles(mustDataDir));
         } else {
            Log.global.warning("Cannot find meta data files for towfish in any of the directories "
                  + consysDataDir + ", "
                  + messorDataDir + ", "
                  + mustDataDir);
            throw new IgnoreModuleComputationException();
         }

         updateTowfishHandleMap(towfishDir);

         setTowfishSegment(getCorrespondingTowfishSegmentHandle(getPingConfiguration().getRawFileConfiguration().getInstant()));
      }

      private void updateTowfishHandleMap(Path towfishDir) throws IOException {
         towfishSegmentHandles.clear();
         List<SegmentHandle> segmentHandles = getComputationContext().getModuleContainer().getKorona().getDataFormatManager().createSegmentHandlesInDirectoryRecursively(towfishDir, getAsyncHandle());
         for (SegmentHandle segmentHandle : segmentHandles) {
            try (PingReader pingReader = segmentHandle.createPingReader()) {
               towfishSegmentHandles.put(pingReader.getPingConfiguration().getRawFileConfiguration().getInstant(), segmentHandle);
            }
         }
      }

      private void updateMetadata() throws IOException {
         if (towfishPingReader == null) {
            return;
         }
         Instant configurationTime = towfishPingReader.getPingConfiguration().getRawFileConfiguration().getInstant().plus(towFishEchosounderAddition);
         // Start time: configurationTime
         // End time: configurationTime plus 3 hours
         towfishMetaData.parseRelevantMetaDataFiles(configurationTime, configurationTime.plus(3, ChronoUnit.HOURS), module.distBehindFunction.getValue());
         beforeLastTowfishPing = towfishPingReader.nextPing(getAsyncHandle());
         lastTowfishPing = towfishPingReader.nextPing(getAsyncHandle());
      }

      private Map.@Nullable Entry<Instant, SegmentHandle> getCorrespondingTowfishSegmentHandle(Instant time) {
         Instant towFishTime = time.minus(towFishEchosounderAddition);
         Map.Entry<Instant, SegmentHandle> handleEntry = towfishSegmentHandles.floorEntry(towFishTime);
         if (handleEntry == null) {
            return towfishSegmentHandles.firstEntry();
         }
         return handleEntry;
      }

      private Map.@Nullable Entry<Instant, SegmentHandle> getNextSegmentHandle(Instant towfishHandleTime) {
         return towfishSegmentHandles.higherEntry(towfishHandleTime);
      }

      @Override
      protected void processPing(Ping ping) throws IOException {
         Ping towfishPing = getTowfishPing(ping);
         if (towfishPing != null) {
            RawMerger.merge(ping, towfishPing, module.transducerBlindZone.getFloatValue());
         }
      }

      @Override
      public void close() throws IOException {
         if (towfishPingReader != null) {
            towfishPingReader.close();
         }
      }

      private @Nullable Ping getTowfishPing(Ping vesselPing) throws IOException {
         if (towfishPingReader == null) {
            return null;
         }

         Instant vesselTime = vesselPing.getInstant();
         Instant correctedVesselTime = vesselTime.minus(towFishEchosounderAddition);
         Instant correspondingTowfishTime = towfishMetaData.getCorrespondingTowfishTime(correctedVesselTime);

         while (lastTowfishPing != null && correspondingTowfishTime.isAfter(lastTowfishPing.getInstant())) {
            Ping towfishPing = towfishPingReader.nextPing(getAsyncHandle()); //read towfish ping corresponding to position of vessel ping
            beforeLastTowfishPing = lastTowfishPing;
            lastTowfishPing = towfishPing;
         }
         // If both last and before-last still have a higher time-stamp than correspondingTowfishTime, return null.
         if (beforeLastTowfishPing != null && beforeLastTowfishPing.getInstant().isAfter(correspondingTowfishTime)) {
            return null;
         }

         // Use the towfish ping with best time match.
         Ping towfishPing = lastTowfishPing != null && beforeLastTowfishPing != null && Math.abs(correspondingTowfishTime.until(lastTowfishPing.getInstant(), ChronoUnit.MILLIS)) < Math.abs(correspondingTowfishTime.until(beforeLastTowfishPing.getInstant(), ChronoUnit.MILLIS))
               ? lastTowfishPing : beforeLastTowfishPing;
         if (towfishPing != null) {
            float towfishDepth = towfishMetaData.getDepth(towfishPing.getInstant());
            PingIndex pingIndex = towfishPing.getPingIndex();
            if (!pingIndex.equals(lastUsedTowfishPingIndex)) {
               addToTransducerDepth(towfishPing, towfishDepth);
            }
            lastUsedTowfishPingIndex = pingIndex;
         }
         if (lastTowfishPing == null) {
            // End of file, select next file if available.
            setTowfishSegment(getNextSegmentHandle(towfishHandleTime));
         }
         return towfishPing;
      }

      private void setTowfishSegment(Map.@Nullable Entry<Instant, SegmentHandle> towfishSegmentHandle) throws IOException {
         if (towfishSegmentHandle != null) {
            towfishHandleTime = towfishSegmentHandle.getKey();
            if (towfishPingReader != null) {
               towfishPingReader.close();
            }
            towfishPingReader = towfishSegmentHandle.getValue().createPingReader();
            updateMetadata();
         }
      }

      private static void addToTransducerDepth(Ping ping, float depth) {
         ping.getNonNullPowerDatas().forEach(powerData -> powerData.setTransducerDepth(powerData.getTransducerDepth() + depth));
      }
   }
}
