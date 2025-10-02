package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.computation.dataquality.RollIndicator;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.DataLoadingMode;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

public final class RollTransmitToReceive extends PingFunction {
   private boolean canBeUsed;

   public RollTransmitToReceive() {
      super(new Name("RollTransmitToReceive", "Roll: degrees-transmit-to-receive"), Unit.DEGREES,
            ExportTransform.round(1000), false,
            "Only in DETAIL mode");
   }

   @Override
   public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
      listenerRegistry.add(lsss.getInterpretationSettings().getDataLoadingModeChangeManager(), mode -> {
         canBeUsed = mode == DataLoadingMode.DETAIL;
         getChangeManager().notifyListeners();
      });
      canBeUsed = lsss.getInterpretationSettings().getDataLoadingMode() == DataLoadingMode.DETAIL;
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      if (!canBeUsed) {
         return Double.NaN;
      }
      ChannelData channelData = ping.getChannelData(channel);
      if (channelData == null) {
         return Double.NaN;
      }
      float roll = channelData.getRoll();
      float pitch = channelData.getPitch();  //Should probably include pitch in returned value
      return roll;
   }

   @Override
   public float[] postprocess(float[] y, long[] timeInMillis, float[] bottom) {
      return RollIndicator.compute(y, timeInMillis, bottom);
   }
}
