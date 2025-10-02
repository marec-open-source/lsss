package no.imr.lsss.modules.misc;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.ComplexChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.ping.items.configuration.TransmitMode;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class NumericalViewModule extends BaseViewModule implements PojoDataContainer {
   private final HeaderParameter textBlocksHeader = new HeaderParameter("Text blocks");

   private final BooleanParameter showRange = new BooleanParameter(
         new Name("Range"),
         true,
         "Info about the echogram ping range");

   private final BooleanParameter showFile = new BooleanParameter(
         new Name("File"),
         true,
         "Info about the file");

   private final BooleanParameter showTransducer = new BooleanParameter(
         new Name("Transducer"),
         true,
         "Info about the transducer");

   private final BooleanParameter showPing = new BooleanParameter(
         new Name("Ping"),
         true,
         "Info about the ping");

   private final BooleanParameter showChannelData = new BooleanParameter(
         new Name("ChannelData", "Channel data"),
         true,
         "Info about the channel data");

   private final BooleanParameter showNmea = new BooleanParameter(
         new Name("NMEA"),
         false,
         "List of NMEA datagrams");

   private final ViewHolder<NumericalViewView> viewHolder = new ViewHolder<>(() -> new NumericalViewView(this));

   private @Nullable Ping ping;
   private String text = "";

   private final Listener recomputeListener = newCoalescingExecListener(this::recompute);

   public NumericalViewModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            textBlocksHeader,
            showRange,
            showFile,
            showTransducer,
            showPing,
            showChannelData,
            showNmea
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(recomputeListener, List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            getInterpretationSettings().getChannelChangeManager()
      ));
      registry.add(getParameters(), recomputeListener);
      registry.add(getInterpretationSettings().mouseover().pingIndex(), newCoalescingExecListener(this::updatePing));

      //---

      updatePing();
   }

   @Override
   protected void onDisable() {
      ping = null;
      text = "";
      viewHolder.removeView();
   }

   private void updatePing() {
      PingIndex pingIndex = getInterpretationSettings().mouseover().getPingIndex();
      ping = pingIndex != null ? getInterpretationSettings().getDataFileSet().getPing(pingIndex) : null;
      recomputeListener.listen();
   }

   private void recompute() {
      text = computeText();
      updateView();
   }

   void updateView() {
      viewHolder.ifViewDelayed(this, view -> view.update(text));
   }

   private String computeText() {
      Ping ping = this.ping;
      int channel = getInterpretationSettings().getChannel();
      List<String> blocks = List.of(
            showRange.getBooleanValue() ? getRangeText(getInterpretationSettings().getPingRange()) : "",
            ping != null && showFile.getBooleanValue() ? getFileText(getInterpretationSettings().getDataFileSet(), ping) : "",
            ping != null && showTransducer.getBooleanValue() ? getTransducerText(ping, channel) : "",
            ping != null && showPing.getBooleanValue() ? getPingText(getInterpretationSettings().getDataFileSet(), ping) : "",
            ping != null && showChannelData.getBooleanValue() ? getChannelDataText(ping, channel) : "",
            ping != null && showNmea.getBooleanValue() ? getNmeaText(ping) : ""
      );
      return blocks.stream()
            .filter(Predicate.not(String::isEmpty))
            .collect(Collectors.joining("\n\n"));
   }

   @Override
   public PojoData getPojoData() {
      Ping ping = this.ping;
      int channel = getInterpretationSettings().getChannel();
      PojoData.Builder builder = PojoData.newBuilder(getPersistentName());
      List<PojoData> blocks = List.of(
            showRange.getBooleanValue() ? getRangePojoData(builder, getInterpretationSettings().getPingRange()) : PojoData.empty(),
            ping != null && showFile.getBooleanValue() ? getFilePojoData(builder, getInterpretationSettings().getDataFileSet(), ping) : PojoData.empty(),
            ping != null && showTransducer.getBooleanValue() ? getTransducerPojoData(builder, ping, channel) : PojoData.empty(),
            ping != null && showPing.getBooleanValue() ? getPingPojoData(builder, getInterpretationSettings().getDataFileSet(), ping) : PojoData.empty(),
            ping != null && showChannelData.getBooleanValue() ? getChannelDataPojoData(builder, ping, channel) : PojoData.empty(),
            ping != null && showNmea.getBooleanValue() ? getNmeaPojoData(builder, ping) : PojoData.empty()
      );
      blocks.forEach(builder::with);
      return builder.build();
   }

   private static String getRangeText(PingRange pingRange) {
      if (pingRange.isEmpty()) {
         return "";
      }
      return "Range: "
            + pingRange.getPingCount() + " pings, "
            + Utils.format("%.3f", pingRange.getVesselDistance()) + " nmi, "
            + pingRange.getDurationString();
   }

   private static PojoData getRangePojoData(PojoData.Builder builder, PingRange pingRange) {
      if (pingRange.isEmpty()) {
         return PojoData.empty();
      }
      return builder.newBuilder()
            .with("range", builder.newBuilder()
                  .with("pings", pingRange.getPingCount())
                  .with("vesselDistance", Unit.NAUTICAL_MILES, ExportRounding.vesselDistance().applyAsDouble(pingRange.getVesselDistance()))
                  .with("duration", pingRange.getDurationString())
                  .build())
            .build();
   }

   private static String getFileText(DataFileSet dataFileSet, Ping ping) {
      DataFile dataFile = dataFileSet.getDataFile(ping.getPingIndex());
      RawFileConfiguration rawFileConfiguration = dataFile.getPingConfiguration().getRawFileConfiguration();
      return "File: " + dataFile.getSegmentHandle().getDisplayName()
            + "\n" + rawFileConfiguration.getInstant()
            + "\nTransducer count\t" + rawFileConfiguration.getTransducerCount();
   }

   private static PojoData getFilePojoData(PojoData.Builder builder, DataFileSet dataFileSet, Ping ping) {
      DataFile dataFile = dataFileSet.getDataFile(ping.getPingIndex());
      RawFileConfiguration rawFileConfiguration = dataFile.getPingConfiguration().getRawFileConfiguration();
      return builder.newBuilder()
            .with("file", builder.newBuilder()
                  .with("name", dataFile.getSegmentHandle().getDisplayName())
                  .with("time", rawFileConfiguration.getInstant().toString())
                  .with("transducerCount", rawFileConfiguration.getTransducerCount())
                  .build())
            .build();
   }

   private static String getTransducerText(Ping ping, int channel) {
      RawFileConfiguration rawFileConfiguration = ping.getRawFileConfiguration();
      if (channel - 1 >= rawFileConfiguration.getTransducers().size()) {
         return "";
      }
      RawFileTransducer transducer = rawFileConfiguration.getTransducers().get(channel - 1);
      ChannelData channelData = ping.getChannelData(channel);
      return "Transducer: " + transducer.getChannelId()
            + "\nName\t\t" + (transducer.getXml0Info() != null ? transducer.getXml0Info().getName() : "")
            + "\nSerial number\t\t" + (transducer.getXml0Info() != null ? transducer.getXml0Info().getTransducerSerialNumber() : "")
            + "\nChannel number\t" + channel
            + "\nFrequency [Hz]\t\t" + Utils.toString(transducer.getFrequency())
            + "\nAngle off. alongship [deg]\t" + transducer.getAngleOffsetAlongship()
            + "\nAngle off. athw.ship [deg]\t" + transducer.getAngleOffsetAthwartship()
            + "\nAngle sens. alongship\t" + transducer.getAngleSensitivityAlongship()
            + "\nAngle sens. athw.ship\t" + transducer.getAngleSensitivityAthwartship()
            + "\nBeam width alongship [deg]\t" + transducer.getBeamWidthAlongship()
            + "\nBeam width athw.ship [deg]\t" + transducer.getBeamWidthAthwartship()
            + "\nEquivalent beam angle [dB]\t" + transducer.getEquivalentBeamAngle()
            + "\nGain [dB]\t\t" + (channelData != null ? transducer.getGainForPulseDuration(channelData.getPulseDuration()) : transducer.getGain())
            + "\nsA correction [dB]\t" + (channelData != null ? transducer.getSaCorrection(channelData.getPulseDuration()) : 0);
   }

   private static PojoData getTransducerPojoData(PojoData.Builder builder, Ping ping, int channel) {
      RawFileConfiguration rawFileConfiguration = ping.getRawFileConfiguration();
      if (channel - 1 >= rawFileConfiguration.getTransducers().size()) {
         return PojoData.empty();
      }
      RawFileTransducer transducer = rawFileConfiguration.getTransducers().get(channel - 1);
      ChannelData channelData = ping.getChannelData(channel);
      return builder.newBuilder()
            .with("transducer", builder.newBuilder()
                  .with("id", transducer.getChannelId())
                  .with("name", transducer.getXml0Info() != null ? transducer.getXml0Info().getName() : "")
                  .with("serialNumber", transducer.getXml0Info() != null ? transducer.getXml0Info().getTransducerSerialNumber() : "")
                  .with("channelNumber", channel)
                  .with("frequency", Unit.HZ, transducer.getFrequency())
                  .with("angleOffsetAlongship", Unit.DEGREES, transducer.getAngleOffsetAlongship())
                  .with("angleOffsetAthwartship", Unit.DEGREES, transducer.getAngleOffsetAthwartship())
                  .with("angleSensitivityAlongship", transducer.getAngleSensitivityAlongship())
                  .with("angleSensitivityAthwartship", transducer.getAngleSensitivityAthwartship())
                  .with("beamWidthAlongship", Unit.DEGREES, transducer.getBeamWidthAlongship())
                  .with("beamWidthAthwartship", Unit.DEGREES, transducer.getBeamWidthAthwartship())
                  .with("equivalentBeamAngle", Unit.DB, transducer.getEquivalentBeamAngle())
                  .with("gain", Unit.DB, channelData != null ? transducer.getGainForPulseDuration(channelData.getPulseDuration()) : transducer.getGain())
                  .with("saCorrection", Unit.DB, channelData != null ? transducer.getSaCorrection(channelData.getPulseDuration()) : 0)
                  .build())
            .build();
   }

   private static String getPingText(DataFileSet dataFileSet, Ping ping) {
      GeoPoint geoPos = ping.getPingIndex().getGeographicalPosition();
      double vesselDistance = dataFileSet.getVesselDistanceUncorrectedForWrapAround(ping.getPingIndex());
      return "Ping: " + ping.getInstant()
            + "\nPing number [#]\t\t" + ping.getPingIndex().getPingNumber()
            + "\nVessel distance [nmi]\t" + Utils.format("%.3f", vesselDistance)
            + "\nVessel speed [knots]\t" + Utils.format("%.3f", DataUtils.getKnots(ping, dataFileSet))
            + "\nLongitude [deg]\t\t" + (geoPos != null ? Utils.format("%.6f", geoPos.getLongitude()) : "")
            + "\nLatitude [deg]\t\t" + (geoPos != null ? Utils.format("%.6f", geoPos.getLatitude()) : "");
   }

   private static PojoData getPingPojoData(PojoData.Builder builder, DataFileSet dataFileSet, Ping ping) {
      GeoPoint geoPos = ping.getPingIndex().getGeographicalPosition();
      double vesselDistance = dataFileSet.getVesselDistanceUncorrectedForWrapAround(ping.getPingIndex());
      return builder.newBuilder()
            .with("ping", builder.newBuilder()
                  .with("time", ping.getInstant().toString())
                  .with("pingNumber", ping.getPingIndex().getPingNumber())
                  .with("vesselDistance", Unit.NAUTICAL_MILES, ExportRounding.vesselDistance().applyAsDouble(vesselDistance))
                  .with("vesselSpeed", Unit.KNOTS, DataUtils.getKnots(ping, dataFileSet))
                  .with("longitude", Unit.DEGREES, ExportRounding.geoPos().applyAsDouble(geoPos != null ? geoPos.getLongitude() : Double.NaN))
                  .with("latitude", Unit.DEGREES, ExportRounding.geoPos().applyAsDouble(geoPos != null ? geoPos.getLatitude() : Double.NaN))
                  .build())
            .build();
   }

   private static String getChannelDataText(Ping ping, int channel) {
      ChannelData channelData = ping.getChannelData(channel);
      if (channelData == null) {
         return "";
      }
      String filterSlope;
      if (channelData instanceof ComplexChannelData complexChannelData) {
         filterSlope = "\nFilter slope\t\t" + complexChannelData.getSlope();
      } else {
         filterSlope = "";
      }
      String frequency;
      String sweep;
      if (channelData instanceof BroadbandData broadbandData) {
         frequency = Utils.toString(broadbandData.getStartFrequency()) + " - " + Utils.toString(broadbandData.getEndFrequency());
         sweep = "\nSweep [Hz/s]\t\t" + broadbandData.getSweep();
      } else {
         frequency = Utils.toString(channelData.getFrequency());
         sweep = "";
      }
      return "Channel data: " + channelData.getInstant()
            + "\nData type\t\t" + channelData.getDataTypeName()
            + "\nFrequency [Hz]\t\t" + frequency
            + sweep
            + filterSlope
            + "\nPulse duration [ms]\t" + channelData.getPulseDuration() * 1000
            + "\nBandwidth [Hz]\t\t" + channelData.getBandWidth()
            + "\nSample interval [ms]\t" + channelData.getSampleInterval() * 1000
            + "\nSample distance [m]\t" + channelData.getSampleDistance()
            + "\nSample count [#]\t" + channelData.getCount()
            + "\nTransducer depth [m]\t" + Utils.format("%.3f", channelData.getTransducerDepth())
            + "\nHeave corr.depth [m]\t" + Utils.format("%.3f", channelData.getHeaveCorrectedTransducerDepth())
            + "\nTransmit power [W]\t" + channelData.getTransmitPower() + " (" + TransmitMode.getTransmitModeString(channelData) + ")"
            + "\nSound velocity [m/s]\t" + channelData.getSoundVelocity()
            + "\nAbsorption [dB/m]\t" + channelData.getAbsorptionCoefficient();
   }

   private static PojoData getChannelDataPojoData(PojoData.Builder builder, Ping ping, int channel) {
      ChannelData channelData = ping.getChannelData(channel);
      if (channelData == null) {
         return PojoData.empty();
      }
      return builder.newBuilder()
            .with("channelData", builder.newBuilder()
                  .with("time", channelData.getInstant().toString())
                  .with("dataType", channelData.getDataTypeName())
                  .withThisBuilder(channelDataBuilder -> {
                     if (channelData instanceof BroadbandData broadbandData) {
                        channelDataBuilder
                              .with("startFrequency", Unit.HZ, broadbandData.getStartFrequency())
                              .with("endFrequency", Unit.HZ, broadbandData.getEndFrequency())
                              .with("sweep", new Unit("Hz/s"), broadbandData.getSweep());
                     } else {
                        channelDataBuilder
                              .with("frequency", Unit.HZ, channelData.getFrequency());
                     }
                  })
                  .withThisBuilder(channelDataBuilder -> {
                     if (channelData instanceof ComplexChannelData complexChannelData) {
                        channelDataBuilder
                              .with("filterSlope", complexChannelData.getSlope());
                     }
                  })
                  .with("pulseDuration", Unit.SECONDS, channelData.getPulseDuration())
                  .with("bandwidth", Unit.HZ, channelData.getBandWidth())
                  .with("sampleInterval", Unit.SECONDS, channelData.getSampleInterval())
                  .with("sampleDistance", Unit.METER, channelData.getSampleDistance())
                  .with("sampleCount", Unit.COUNT, channelData.getCount())
                  .with("transducerDepth", Unit.METER, channelData.getTransducerDepth())
                  .with("heaveCorrectedTransducerDepth", Unit.METER, channelData.getHeaveCorrectedTransducerDepth())
                  .with("transmitPower", Unit.WATT, channelData.getTransmitPower())
                  .with("transmitMode", TransmitMode.getTransmitModeString(channelData))
                  .with("soundVelocity", Unit.METER_PER_SECOND, channelData.getSoundVelocity())
                  .with("absorption", Unit.DB_PER_METER, channelData.getAbsorptionCoefficient())
                  .build())
            .build();
   }

   private static String getNmeaText(Ping ping) {
      List<String> nmeaStrings = ping.getPingItems(NmeaPingItem.class)
            .map(NmeaPingItem::getNmeaString)
            .toList();
      return "NMEA datagrams (" + nmeaStrings.size() + "):\n"
            + String.join("\n", nmeaStrings);
   }

   private static PojoData getNmeaPojoData(PojoData.Builder builder, Ping ping) {
      List<String> nmeaStrings = ping.getPingItems(NmeaPingItem.class)
            .map(NmeaPingItem::getNmeaString)
            .toList();
      return builder.newBuilder()
            .with("nmea", nmeaStrings)
            .build();
   }
}
