package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.computation.noise.NoiseFile;
import no.imr.korona.computation.noise.NoiseQuantificationModule;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.NavigableMap;
import java.util.function.ToDoubleFunction;

public final class NoiseXmlFunction extends PingFunction {
   private final ToDoubleFunction<NoiseFile.NoiseData> function;
   private @Nullable InterpretationSettings interpretationSettings;
   private @Nullable Path noiseXml;
   private @Nullable NoiseFile noiseFile;
   private long lastModified;
   private long nextCheckTime;

   private NoiseXmlFunction(Name name, Unit unit, ExportTransform exportTransform, ToDoubleFunction<NoiseFile.NoiseData> function) {
      super(name, unit, exportTransform, true);

      this.function = function;
   }

   @Override
   public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
      interpretationSettings = lsss.getInterpretationSettings();
      listenerRegistry.add(interpretationSettings.getDataFileChangeManager(), this::readNoiseXml);
   }

   private void readNoiseXml() {
      assert interpretationSettings != null;
      DataFileSet dataFileSet = interpretationSettings.getDataFileSet();
      if (dataFileSet.isEmpty()) {
         noiseXml = null;
         noiseFile = null;
         lastModified = 0;
      } else {
         RawFileConfiguration rawFileConfiguration = dataFileSet.getRawFileConfiguration();
         noiseXml = rawFileConfiguration.getDataFile().resolveSibling(NoiseQuantificationModule.NOISE_XML);
         noiseFile = new NoiseFile(rawFileConfiguration, noiseXml, Integer.MAX_VALUE);
         lastModified = FileUtils.lastModifiedOr0(noiseXml);
      }
      getChangeManager().notifyListeners();
   }

   private void checkForUpdate() {
      nextCheckTime = System.currentTimeMillis() + 2000;
      if (noiseXml == null) {
         return;
      }
      if (lastModified != FileUtils.lastModifiedOr0(noiseXml)) {
         readNoiseXml();
      }
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      if (System.currentTimeMillis() > nextCheckTime) {
         checkForUpdate();
      }

      if (noiseFile == null) {
         return Double.NaN;
      }

      NavigableMap<Integer, NoiseFile.NoiseData> noiseMapForTime = noiseFile.timeToNoiseMap(ping.getTimeInMillis());
      if (noiseMapForTime == null) {
         return Double.NaN;
      }

      RawFileTransducer transducer = ping.getRawFileConfiguration().getTransducers().get(channel - 1);
      int kHz = transducer.getKHz();
      NoiseFile.NoiseData noiseData = noiseMapForTime.get(kHz);
      if (noiseData == null) {
         return Double.NaN;
      }

      return function.applyAsDouble(noiseData);
   }

   public static PingFunction average() {
      return new NoiseXmlFunction(new Name("noiseFallbackAverage", "Noise fallback: Average"), Unit.DB, ExportRounding.db(),
            noiseData -> PowerData.svToLogSv(noiseData.ne()));
   }

   public static PingFunction upperLimit() {
      return new NoiseXmlFunction(new Name("noiseFallbackUpperLimit", "Noise fallback: Upper limit"), Unit.DB, ExportRounding.db(),
            noiseData -> PowerData.svToLogSv(noiseData.nh()));
   }

   public static PingFunction quality() {
      return new NoiseXmlFunction(new Name("noiseFallbackQuality", "Noise fallback: Quality"), Unit.DIMENSIONLESS, ExportTransform.round(100),
            NoiseFile.NoiseData::quality);
   }
}
