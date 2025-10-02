package no.imr.korona.data.ping.items.configuration;

import no.imr.korona.computation.broadband.PulseCompressionFilter;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterConfig;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterUtils;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFiltersFileService;
import no.imr.korona.computation.broadband.pulsecompressionfilter.PulseCompressionFilterConfig;
import no.imr.korona.computation.broadband.pulsecompressionfilter.PulseCompressionFilterUtils;
import no.imr.korona.computation.broadband.pulsecompressionfilter.PulseCompressionFiltersFileService;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Con0Datagram;
import no.imr.korona.data.datagrams.Fil1Datagram;
import no.imr.korona.data.datagrams.LsssDatagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.datagrams.subdatagrams.config.ExtraRawFileConfiguration;
import no.imr.korona.data.formats.ek60.Xml0DatagramFactory;
import no.imr.korona.data.formats.ek60.calibration.CalibrationEntry;
import no.imr.korona.data.formats.ek60.calibration.CalibrationFile;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.AbstractPingItem;
import no.imr.korona.data.track.RawFileConfigurationInfo;
import no.imr.korona.util.absorption.Absorption;
import no.imr.korona.util.absorption.FrancoisGarrisonAbsorption;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.math.linalg.Vec3;
import no.marec.lsss.api.data.ChannelConfiguration;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Raw file configuration.
 */
public final class RawFileConfiguration extends AbstractPingItem implements no.marec.lsss.api.data.PingConfiguration {
   private String surveyName = ""; // "Loch Ness" (128)
   private String transectName = ""; // "L0123"  (128)
   private String sounderName = ""; // "EK60" (128)
   private String version = ""; // "1.2.3.45" (30)
   private short multiplexing; // 0=Normal, 1=Mux
   private int timeBias; //diff between UTC and local time [minutes]
   private float soundVelocityAverage; // [m/s]
   private float soundVelocityTransducer; // [m/s]
   private Vec3 mruOffset = Vec3.ZERO; // [m]
   private Vec3 mruAlpha = Vec3.ZERO; // [deg]
   private Vec3 gpsOffset = Vec3.ZERO; // [m]
   // private String spare = ""; // future use (48)
   private List<RawFileTransducer> transducers = new ArrayList<>(); // Transducer settings

   private final List<String> notices = new ArrayList<>();
   private Set<String> ignoredChannelIds = Set.of();

   private @Nullable Path dataFile;
   private CalibrationFile calibrationFile = CalibrationFile.EMPTY;
   private List<BroadbandNotchFilterConfig> broadbandNotchFilterConfigs = List.of();
   private Map<String, List<PulseCompressionFilterConfig>> pulseCompressionFilterMap = Map.of();

   private @Nullable Xml0Info xml0Info;

   public RawFileConfiguration(long ntDate) {
      super(ntDate);
   }

   public RawFileConfiguration(RawFileConfiguration rawFileConfiguration) {
      super(rawFileConfiguration.getNTDate());

      surveyName = rawFileConfiguration.surveyName;
      transectName = rawFileConfiguration.transectName;
      sounderName = rawFileConfiguration.sounderName;
      version = rawFileConfiguration.version;
      multiplexing = rawFileConfiguration.multiplexing;
      timeBias = rawFileConfiguration.timeBias;
      soundVelocityAverage = rawFileConfiguration.soundVelocityAverage;
      soundVelocityTransducer = rawFileConfiguration.soundVelocityTransducer;
      mruOffset = rawFileConfiguration.mruOffset;
      mruAlpha = rawFileConfiguration.mruAlpha;
      gpsOffset = rawFileConfiguration.gpsOffset;
      // spare = rawFileConfiguration.spare;
      transducers = rawFileConfiguration.transducers.stream()
            .map(RawFileTransducer::makeCopy)
            .collect(Collectors.toList());

      notices.addAll(rawFileConfiguration.notices);
      ignoredChannelIds = rawFileConfiguration.ignoredChannelIds;
      dataFile = rawFileConfiguration.dataFile;
      calibrationFile = rawFileConfiguration.calibrationFile;
      broadbandNotchFilterConfigs = rawFileConfiguration.broadbandNotchFilterConfigs;
      pulseCompressionFilterMap = rawFileConfiguration.pulseCompressionFilterMap;
      xml0Info = rawFileConfiguration.xml0Info;
   }

   public RawFileConfiguration(Con0Datagram con0Datagram) {
      super(con0Datagram.getNTDate());

      surveyName = con0Datagram.surveyName;
      transectName = con0Datagram.transectName;
      sounderName = con0Datagram.sounderName;
      version = con0Datagram.version;
      multiplexing = con0Datagram.multiplexing;
      timeBias = con0Datagram.timeBias;
      soundVelocityAverage = con0Datagram.soundVelocityAverage;
      soundVelocityTransducer = con0Datagram.soundVelocityTransducer;
      mruOffset = con0Datagram.mruOffset;
      mruAlpha = con0Datagram.mruAlpha;
      gpsOffset = con0Datagram.gpsOffset;
      // spare = con0Datagram.spare;
      transducers = con0Datagram.transducers.stream()
            .map(RawFileTransducer::new)
            .collect(Collectors.toList());
   }

   private Con0Datagram toCon0Datagram() {
      Con0Datagram con0Datagram = new Con0Datagram(getNTDate());
      con0Datagram.surveyName = surveyName;
      con0Datagram.transectName = transectName;
      con0Datagram.sounderName = sounderName;
      con0Datagram.version = version;
      con0Datagram.multiplexing = multiplexing;
      con0Datagram.timeBias = timeBias;
      con0Datagram.soundVelocityAverage = soundVelocityAverage;
      con0Datagram.soundVelocityTransducer = soundVelocityTransducer;
      con0Datagram.mruOffset = mruOffset;
      con0Datagram.mruAlpha = mruAlpha;
      con0Datagram.gpsOffset = gpsOffset;
      // con0Datagram.spare = spare;
      con0Datagram.transducers = transducers.stream()
            .map(RawFileTransducer::toCon0DatagramTransducer)
            .collect(Collectors.toList());
      return con0Datagram;
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      List<BaseDatagram> datagrams = new ArrayList<>();
      if (xml0Info != null) {
         datagrams.add(Xml0DatagramFactory.toXml0Configuration(this));
         datagrams.add(Xml0DatagramFactory.toXml0Environment(this));
         Xml0Datagram xml0InitialParameters = Xml0DatagramFactory.toXml0InitialParameter(this);
         if (xml0InitialParameters != null) {
            datagrams.add(xml0InitialParameters);
         }
      } else {
         datagrams.add(toCon0Datagram());
      }

      LsssDatagram extraRawFileConfigurationDatagram = toExtraRawFileConfiguration();
      if (extraRawFileConfigurationDatagram != null) {
         datagrams.add(extraRawFileConfigurationDatagram);
      }
      for (int i = 0; i < transducers.size(); i++) {
         RawFileTransducer transducer = transducers.get(i);
         int channel = i + 1;
         transducer.getPulseCompressionFilterChain().getFilters().stream()
               .map(filter -> toFil1Datagram(getNTDate(), channel, transducer.getChannelId(), filter))
               .forEach(datagrams::add);
      }
      return datagrams;
   }

   private @Nullable LsssDatagram toExtraRawFileConfiguration() {
      Element element = DocumentHelper.createElement(ExtraRawFileConfiguration.XML_ROOT);
      if (!broadbandNotchFilterConfigs.isEmpty()) {
         element.add(BroadbandNotchFilterUtils.toXml(broadbandNotchFilterConfigs));
      }
      if (!pulseCompressionFilterMap.isEmpty()) {
         element.add(PulseCompressionFilterUtils.toXml(pulseCompressionFilterMap));
      }
      if (element.elements().isEmpty()) {
         return null;
      }
      return new LsssDatagram(new ExtraRawFileConfiguration(getNTDate(), element));
   }

   private static Fil1Datagram toFil1Datagram(long ntDate, int channel, String channelId, PulseCompressionFilter filter) {
      Fil1Datagram fil1Datagram = new Fil1Datagram(ntDate);
      fil1Datagram.stage = (short) filter.stage();
      fil1Datagram.channel = (short) channel;
      fil1Datagram.channelId = channelId;
      fil1Datagram.decimationFactor = (short) filter.decimationFactor();
      fil1Datagram.coefficients = filter.coefficients();
      return fil1Datagram;
   }

   public void applyExtraRawFileConfiguration(ExtraRawFileConfiguration extraRawFileConfiguration) {
      Element notchFilterXml = extraRawFileConfiguration.getElement().element(BroadbandNotchFiltersFileService.BROADBAND_NOTCH_FILTER_XML);
      if (notchFilterXml != null) {
         broadbandNotchFilterConfigs = BroadbandNotchFilterUtils.fromXml(notchFilterXml);
      }
      Element pulseCompressionFilterXml = extraRawFileConfiguration.getElement().element(PulseCompressionFiltersFileService.PULSE_COMPRESSION_FILTER_XML);
      if (pulseCompressionFilterXml != null) {
         pulseCompressionFilterMap = PulseCompressionFilterUtils.fromXml(pulseCompressionFilterXml);
      }
   }

   @Override
   public String toString() {
      return super.toString() + " \"" + surveyName + "\" \"" + transectName + "\" \""
            + sounderName + "\" \"" + version + "\" "
            + transducers.stream().map(transducer -> Utils.toString(transducer.getFrequency())).collect(Collectors.joining(", ", "[", "]"));
   }

   public List<String> getNotices() {
      return notices;
   }

   public Set<String> getIgnoredChannelIds() {
      return ignoredChannelIds;
   }

   public void setIgnoredChannelIds(Set<String> ignoredChannelIds) {
      this.ignoredChannelIds = ignoredChannelIds;
   }

   public Path getDataFile() {
      if (dataFile == null) {
         throw new IllegalStateException();
      }
      return dataFile;
   }

   public void setDataFile(Path dataFile) {
      this.dataFile = dataFile;
   }

   public CalibrationFile getCalibrationFile() {
      return calibrationFile;
   }

   public void setCalibrationFile(CalibrationFile calibrationFile) {
      this.calibrationFile = calibrationFile;
      calibrate(calibrationFile.getContent().getDefaultType().getEntry(this));
   }

   private void calibrate(CalibrationEntry calibrationEntry) {
      for (int channel = 1; channel <= transducers.size(); channel++) {
         RawFileTransducer transducer = transducers.get(channel - 1);
         calibrationEntry.getChannelCalibration(transducer, channel)
               .ifPresentOrElse(transducer::calibrate, Log.global::warning);
      }
   }

   public String getSurveyName() {
      return surveyName;
   }

   public void setSurveyName(String surveyName) {
      this.surveyName = surveyName;
   }

   public String getTransectName() {
      return transectName;
   }

   public void setTransectName(String transectName) {
      this.transectName = transectName;
   }

   public String getSounderName() {
      return sounderName;
   }

   public void setSounderName(String sounderName) {
      this.sounderName = sounderName;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   @Override
   public List<? extends ChannelConfiguration> getChannelConfigurations() {
      return transducers;
   }

   public List<RawFileTransducer> getTransducers() {
      return transducers;
   }

   public short getMultiplexing() {
      return multiplexing;
   }

   public void setMultiplexing(short multiplexing) {
      this.multiplexing = multiplexing;
   }

   public int getTimeBias() {
      return timeBias;
   }

   public void setTimeBias(int timeBias) {
      this.timeBias = timeBias;
   }

   public float getSoundVelocityAverage() {
      return soundVelocityAverage;
   }

   public void setSoundVelocityAverage(float soundVelocityAverage) {
      this.soundVelocityAverage = soundVelocityAverage;
   }

   public float getSoundVelocityTransducer() {
      return soundVelocityTransducer;
   }

   public void setSoundVelocityTransducer(float soundVelocityTransducer) {
      this.soundVelocityTransducer = soundVelocityTransducer;
   }

   public Vec3 getMRUOffset() {
      return mruOffset;
   }

   public void setMRUOffset(Vec3 mruOffset) {
      this.mruOffset = mruOffset;
   }

   public Vec3 getMRUAlpha() {
      return mruAlpha;
   }

   public void setMRUAlpha(Vec3 mruAlpha) {
      this.mruAlpha = mruAlpha;
   }

   public Vec3 getGPSOffset() {
      return gpsOffset;
   }

   public void setGPSOffset(Vec3 gpsOffset) {
      this.gpsOffset = gpsOffset;
   }

   public int getTransducerCount() {
      return transducers.size();
   }

   /**
    * Adds a new channel.
    * The new channel's number is one more than the previous highest channel number.
    *
    * @param initializer source for the new channel's RawFileTransducer
    * @return the copied RawFileTransducer
    */
   public RawFileTransducer newChannel(RawFileTransducer initializer) {
      RawFileTransducer transducerCopy = initializer.makeCopy();
      transducerCopy.initAsCopy();
      transducers.add(transducerCopy);
      return transducerCopy;
   }

   /**
    * Removes a channel.
    *
    * @param channel the channel to remove
    * @return true if removed, false if channel is invalid
    */
   public boolean removeChannel(int channel) {
      if (channel <= 0 || channel > getTransducerCount()) {
         return false;
      }

      transducers.remove(channel - 1);
      return true;
   }

   /**
    * Removes all channels.
    */
   public void clearChannels() {
      transducers.clear();
   }

   @Override
   public RawFileConfiguration makeCopy() {
      return new RawFileConfiguration(this);
   }

   public int channelIdToChannelIndex(String channelId) {
      for (int i = 0; i < transducers.size(); i++) {
         RawFileTransducer transducer = transducers.get(i);
         if (transducer.getChannelId().equals(channelId)) {
            return i;
         }
      }
      return -1;
   }

   /**
    * {@return the last channel of the specified frequency, or -1 if none found}
    *
    * @param kHz a frequency in kHz
    */
   public int lastChannelWithKHz(int kHz) {
      for (int channel = transducers.size(); channel > 0; channel--) {
         if (transducers.get(channel - 1).getKHz() == kHz) {
            return channel;
         }
      }
      return -1;
   }

   /**
    * {@return the closest channel within 1 kHz, or -1 if none found}
    * If multiple frequencies are equally close, the first is returned.
    *
    * @param frequency a frequency in Hz
    */
   public int firstChannelClosestTo(float frequency) {
      return firstChannelClosestTo(frequency, 1000);
   }

   /**
    * {@return the closest channel within the given frequency threshold, or -1 if none found}
    * If multiple frequencies are equally close, the first is returned.
    *
    * @param frequency a frequency in Hz
    * @param maxDiff   the maximum allowed difference
    */
   public int firstChannelClosestTo(float frequency, float maxDiff) {
      int closestChannel = -1;
      float minDiff = Math.nextUp(maxDiff);

      for (int channel = 1; channel <= transducers.size(); channel++) {
         float f = transducers.get(channel - 1).getFrequency();
         float diff = Math.abs(frequency - f);
         if (diff < minDiff) {
            minDiff = diff;
            closestChannel = channel;
         }
      }

      return closestChannel;
   }

   @Override
   public @Nullable String getIncompatibility(PingConfiguration pingConfiguration) {
      List<RawFileTransducer> otherTransducers = pingConfiguration.getRawFileConfiguration().transducers;
      if (transducers.size() != otherTransducers.size()) {
         return "Different number of transducers";
      }
      for (int i = 0; i < transducers.size(); i++) {
         if (transducers.get(i).getFrequency() != otherTransducers.get(i).getFrequency()) {
            return "Different set of frequencies";
         }
      }
      return null;
   }

   public @Nullable String getIncompatibility(RawFileConfigurationInfo rawFileConfigurationInfo) {
      float[] otherFrequencies = rawFileConfigurationInfo.frequencies;
      if (transducers.size() != otherFrequencies.length) {
         return "Different number of transducers";
      }
      for (int i = 0; i < transducers.size(); i++) {
         if (transducers.get(i).getFrequency() != otherFrequencies[i]) {
            return "Different set of frequencies";
         }
      }
      return null;
   }

   public List<BroadbandNotchFilterConfig> getBroadbandNotchFilterConfigs() {
      return broadbandNotchFilterConfigs;
   }

   public Map<String, List<PulseCompressionFilterConfig>> getPulseCompressionFilterMap() {
      return pulseCompressionFilterMap;
   }

   public void possiblyAddBroadbandNotchFilterConfigs(List<BroadbandNotchFilterConfig> configs) {
      Set<BroadbandNotchFilterConfig> s = new HashSet<>(broadbandNotchFilterConfigs);
      s.addAll(configs);
      broadbandNotchFilterConfigs = List.copyOf(s);
   }

   public void setPulseCompressionFilterMap(Map<String, List<PulseCompressionFilterConfig>> configMap) {
      pulseCompressionFilterMap = Map.copyOf(configMap);
   }

   public @Nullable Xml0Info getXml0Info() {
      return xml0Info;
   }

   public void setXml0Info(Xml0Info xml0Info) {
      this.xml0Info = xml0Info;
   }

   /**
    * Extra info in XML0/Configuration and XML0/Environment that is not in CON0.
    */
   public static final class Xml0Info {
      private float depth;
      private float acidity;
      private float salinity;
      private float temperature;
      private float dropKeelOffset;
      private float waterLevelDraft;
      private @Nullable Absorption absorption;

      public Xml0Info() {
      }

      public float getDepth() {
         return depth;
      }

      public void setDepth(float depth) {
         this.depth = depth;
         absorption = null;
      }

      public float getAcidity() {
         return acidity;
      }

      public void setAcidity(float acidity) {
         this.acidity = acidity;
         absorption = null;
      }

      public float getSalinity() {
         return salinity;
      }

      public void setSalinity(float salinity) {
         this.salinity = salinity;
         absorption = null;
      }

      public float getTemperature() {
         return temperature;
      }

      public void setTemperature(float temperature) {
         this.temperature = temperature;
         absorption = null;
      }

      public float getDropKeelOffset() {
         return dropKeelOffset;
      }

      public void setDropKeelOffset(float dropKeelOffset) {
         this.dropKeelOffset = dropKeelOffset;
      }

      public float getWaterLevelDraft() {
         return waterLevelDraft;
      }

      public void setWaterLevelDraft(float waterLevelDraft) {
         this.waterLevelDraft = waterLevelDraft;
      }

      public Absorption getAbsorption() {
         Absorption abs = absorption;
         if (abs == null) {
            abs = new FrancoisGarrisonAbsorption(acidity, salinity, temperature, depth);
            absorption = abs;
         }
         return abs;
      }
   }
}
