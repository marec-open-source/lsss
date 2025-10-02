package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.IgnoreModuleComputationException;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntCsvListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.io.IOException;
import java.util.List;

public final class ChannelRemovalModule extends ConcurrentPingModule {
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

   public final ObjectParameter<ChannelDataRemovalModule.DataTypeEnum> dataType = new ObjectParameter<>(
         new Name("DataType", "Data type"),
         ChannelDataRemovalModule.DataTypeEnum.NOT_SPECIFIED, ChannelDataRemovalModule.DataTypeEnum.values(),
         "The data type to select. NB: Only the first few pings will determine if a channel is selected");

   public final ObjectParameter<ChannelDataRemovalModule.TransmitModeEnum> transmitMode = new ObjectParameter<>(
         new Name("TransmitMode", "Transmit mode"),
         ChannelDataRemovalModule.TransmitModeEnum.NOT_SPECIFIED, ChannelDataRemovalModule.TransmitModeEnum.values(),
         "The transmit mode to select. NB Only the first few pings will determine if a channel is selected");

   public final StringParameter pingId = new StringParameter(
         new Name("PingId", "Ping ID"),
         "",
         "Used in connection with advanced sequencing of different pulse types");

   public final BooleanParameter keepSpecified = new BooleanParameter(
         new Name("KeepSpecified", "Keep specified"),
         false,
         "If selected, then keep only the specified channels");

   public ChannelRemovalModule() {
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
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException {
      return new ChannelRemovalModuleComputation(this, computationContext, pingSource);
   }
}
