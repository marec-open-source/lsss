package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.NarrowbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.ping.items.configuration.TransmitMode;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntCsvListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ChannelDataRemovalModule extends ConcurrentPingModule {
   public final IntCsvListParameter channels = new IntCsvListParameter(
         new Name("Channels"),
         List.of(), Unit.NONE, ValueConstraints.gt(0),
         "Comma-separated list of channel numbers");

   public final IntCsvListParameter channelsFromEnd = new IntCsvListParameter(
         new Name("ChannelsFromEnd", "Channels from end"),
         List.of(), Unit.NONE, ValueConstraints.gt(0),
         "Comma-separated list of channel numbers from the end. (1 = last channel, 2 = second last channel, etc.)");

   public final IntCsvListParameter frequencies = new IntCsvListParameter(
         new Name("Frequencies"),
         List.of(), Unit.KHZ, ValueConstraints.gt(0),
         "Comma-separated list of kHz");

   public final ObjectParameter<DataTypeEnum> dataType = new ObjectParameter<>(
         new Name("DataType", "Data type"),
         DataTypeEnum.NOT_SPECIFIED, DataTypeEnum.values(),
         "The selected data type");

   public final ObjectParameter<TransmitModeEnum> transmitMode = new ObjectParameter<>(
         new Name("TransmitMode", "Transmit mode"),
         TransmitModeEnum.NOT_SPECIFIED, TransmitModeEnum.values(),
         "The selected transmit mode");

   public final StringParameter pingId = new StringParameter(
         new Name("PingId", "Ping ID"),
         "",
         "Incubating feature: Used in connection with advanced sequencing of different pulse types");

   public final BooleanParameter keepSpecified = new BooleanParameter(
         new Name("KeepSpecified", "Keep specified"),
         false,
         "If selected, then keep data on only the specified channels");

   public ChannelDataRemovalModule() {
      pingId.setVisible(KoronaIncubatorFeatureToggles.CHANNEL_REMOVAL_PING_ID);
      pingId.setPersistable(KoronaIncubatorFeatureToggles.CHANNEL_REMOVAL_PING_ID);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            channels,
            channelsFromEnd,
            frequencies,
            dataType,
            transmitMode,
            pingId,
            keepSpecified
      );
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new ChannelDataRemovalModuleComputation(this, computationContext, pingSource);
   }

   static Set<Integer> selectedChannels(PingConfiguration pingConfiguration,
                                        IntCsvListParameter channels,
                                        IntCsvListParameter channelsFromEnd,
                                        IntCsvListParameter frequencies,
                                        StringParameter pingId) {
      List<RawFileTransducer> transducers = pingConfiguration.getRawFileConfiguration().getTransducers();
      int transducerCount = transducers.size();
      Set<Integer> selectedChannels = new HashSet<>();

      channels.getValue().stream()
            .filter(channel -> channel > 0 && channel <= transducerCount)
            .forEach(selectedChannels::add);

      channelsFromEnd.getValue().stream()
            .mapToInt(channelFromEnd -> transducerCount - channelFromEnd + 1)
            .filter(channel -> channel > 0 && channel <= transducerCount)
            .forEach(selectedChannels::add);

      Set<Integer> frequencySet = new HashSet<>(frequencies.getValue());
      for (int i = 0; i < transducerCount; i++) {
         if (frequencySet.contains(transducers.get(i).getKHz())) {
            selectedChannels.add(i + 1);
         }
      }

      String thePingId = pingId.getValue();
      if (!thePingId.isEmpty()) {
         for (int i = 0; i < transducerCount; i++) {
            RawFileTransducer.InitialParameters initialParameters = transducers.get(i).getXml0InitialParameters();
            if (initialParameters != null && thePingId.equals(initialParameters.pingId())) {
               selectedChannels.add(i + 1);
            }
         }
      }

      return selectedChannels;
   }

   static Set<Integer> invertChannels(PingConfiguration pingConfiguration, Set<Integer> channels) {
      return IntStream.rangeClosed(1, pingConfiguration.getRawFileConfiguration().getTransducerCount())
            .boxed()
            .filter(Predicate.not(channels::contains))
            .collect(Collectors.toSet());
   }

   public enum DataTypeEnum implements ObjectParameterValue {
      NOT_SPECIFIED("", "Not specified"),
      CW_REAL_DATA("CW - Real data", "Real data with power and angles"),
      CW_COMPLEX_DATA("CW - Complex data", "Complex narrowband data"),
      FM_COMPLEX_DATA("FM - Complex data", "Complex broadband data");

      private final String label;
      private final String tooltip;

      DataTypeEnum(String label, String tooltip) {
         this.label = label;
         this.tooltip = tooltip;
      }

      @Override
      public String toString() {
         return this == NOT_SPECIFIED ? "" : name();
      }

      @Override
      public String getDisplayLabel() {
         return label;
      }

      @Override
      public String getTooltip() {
         return tooltip;
      }

      Optional<Predicate<ChannelData>> predicate() {
         return Optional.ofNullable(switch (this) {
            case NOT_SPECIFIED -> null;
            case CW_REAL_DATA -> PowerData.class::isInstance;
            case CW_COMPLEX_DATA -> NarrowbandData.class::isInstance;
            case FM_COMPLEX_DATA -> BroadbandData.class::isInstance;
         });
      }
   }

   public enum TransmitModeEnum implements ObjectParameterValue {
      NOT_SPECIFIED("", "Not specified"),
      ACTIVE("Active", "Active mode"),
      PASSIVE("Passive", "Passive mode");

      private final String label;
      private final String tooltip;

      TransmitModeEnum(String label, String tooltip) {
         this.label = label;
         this.tooltip = tooltip;
      }

      @Override
      public String toString() {
         return this == NOT_SPECIFIED ? "" : name();
      }

      @Override
      public String getDisplayLabel() {
         return label;
      }

      @Override
      public String getTooltip() {
         return tooltip;
      }

      Optional<Predicate<ChannelData>> predicate() {
         return Optional.ofNullable(switch (this) {
            case NOT_SPECIFIED -> null;
            case ACTIVE -> channelData -> channelData.getTransmitMode() == TransmitMode.ACTIVE;
            case PASSIVE -> channelData -> channelData.getTransmitMode() == TransmitMode.PASSIVE;
         });
      }
   }
}
