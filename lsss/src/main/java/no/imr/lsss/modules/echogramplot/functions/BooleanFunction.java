package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.TransmitMode;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

public abstract class BooleanFunction extends PingFunction {
   private BooleanFunction(Name name) {
      super(name, Unit.NONE, ExportTransform.identity(), true);
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      return computeAsBoolean(dataFileSet, ping, channel) ? 1 : 0;
   }

   public abstract boolean computeAsBoolean(DataFileSet dataFileSet, Ping ping, int channel);

   public static BooleanFunction active() {
      return new BooleanFunction(new Name("indicatorActive", "Indicator: Active")) {
         @Override
         public boolean computeAsBoolean(DataFileSet dataFileSet, Ping ping, int channel) {
            ChannelData channelData = ping.getChannelData(channel);
            return channelData != null && channelData.getTransmitMode() == TransmitMode.ACTIVE;
         }
      };
   }

   public static BooleanFunction broadband() {
      return new BooleanFunction(new Name("indicatorBroadband", "Indicator: Broadband")) {
         @Override
         public boolean computeAsBoolean(DataFileSet dataFileSet, Ping ping, int channel) {
            return ping.getChannelData(channel) instanceof BroadbandData;
         }
      };
   }
}
