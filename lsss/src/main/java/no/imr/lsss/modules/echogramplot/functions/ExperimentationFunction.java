package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.datagrams.subdatagrams.plot.PlotParameterValueSubDatagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.util.TvgArray;
import no.imr.korona.region.Region;
import no.imr.lsss.LSSS;
import no.imr.tools.Max;
import no.imr.tools.Utils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.ArrayKernel;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class ExperimentationFunction extends PingFunction {
   public static final boolean USE = !Utils.IS_DIST_VERSION;

   private final LSSS lsss;
   private final int n;

   public ExperimentationFunction(LSSS lsss, int n) {
      super(new Name("ExperimentationFunction-" + n), Unit.DIMENSIONLESS, ExportTransform.round(1000));

      this.lsss = lsss;
      this.n = n;
   }

   @Override
   public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
      switch (n) {
         case 1 -> {
            listenerRegistry.add(getChangeManager(), List.of(
                  lsss.getRegionManager().getRegionDefinitionChangeManager(),
                  lsss.getRegionManager().selectedRegions()
            ));
         }
         //case 2 -> ;
         default -> {
         }
      }
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      ChannelData channelData = ping.getChannelData(channel);
      if (channelData == null) {
         return Double.NaN;
      }
      return switch (n) {
         case 1 -> compute1(ping, channelData);
         case 2 -> compute2(channelData);
         default -> Double.NaN;
      };
   }

   private double compute1(Ping ping, ChannelData channelData) {
      //Float spikeCount = getPlotParameterValue(ping, channelData.getChannel(), "SpikeFilterModule.spikeCount");
      //Float spikeCount = getPlotParameterValue(ping, channelData.getChannel(), "BubblSpikeFilterModule.spikeCount");
      //if (spikeCount != null) {
      //   return spikeCount;
      //}

      Nqp0Datagram nqp0Datagram = ping.getPingItems(Nqp0Datagram.class)
            .filter(datagram -> datagram.getChannel() == channelData.getChannel())
            .findFirst()
            .orElse(null);

      if (nqp0Datagram == null) {
         return Double.NaN;
      }

      float nh = nqp0Datagram.getUpperLimit();

      int sampleCountAboveNh = 0;
      int sampleCountTotal = 0;

      float[] sv = channelData.getPowerData().getSv();
      TvgArray tvg = channelData.getTVGArray();

      for (Region region : lsss.getRegionManager().getSelectedRegions()) {
         FloatRangeSet depthRangeSet = lsss.getRegionManager().getDepthRangesForChannel(region, ping, channelData.getChannel());
         for (FloatRange depthRange : depthRangeSet) {
            int iMin = channelData.depthToClampedSampleIndex(depthRange.min());
            int iMax = channelData.depthToClampedSampleIndex(depthRange.max());
            for (int i = iMin; i < iMax; i++) {
               float n = sv[i] / tvg.get(i);
               if (n > nh) {
                  sampleCountAboveNh++;
               }
               sampleCountTotal++;
            }
         }
      }
      if (sampleCountTotal == 0) {
         return Double.NaN;
      }

      return 100.0 * sampleCountAboveNh / sampleCountTotal;
   }

   private static double compute2(ChannelData channelData) {
      //float pitch = channelData.getPitch();
      //return pitch * pitch;
      float roll = channelData.getRoll();
      return roll * roll;
   }

   private static @Nullable Float getPlotParameterValue(Ping ping, int channel, String id) {
      PlotParameterValueSubDatagram valueSubDatagram = ping.getPingItem(PlotParameterValueSubDatagram.class);
      if (valueSubDatagram == null) {
         return null;
      }
      Map<Integer, Float> channelMap = valueSubDatagram.getPerChannelValues().get(id);
      if (channelMap == null) {
         return null;
      }
      return channelMap.get(channel);
   }

   @Override
   public float[] postprocess(float[] y, long[] timeInMillis, float[] bottom) {
      return switch (n) {
         case 1 -> postprocess1(y, timeInMillis, bottom);
         case 2 -> postprocess2(y, timeInMillis, bottom);
         default -> y;
      };
   }

   private static float[] postprocess1(float[] y, long[] timeInMillis, float[] bottom) {
      y = ArrayKernel.createGaussian(3).smooth(y);
      float[] yCopy = y.clone();
      int maxRadius = 100;  //For 1 sec ping-rate, 60 means 2 minutes
      float soundSpeed = 1500; //For now

      for (int i = 0; i < y.length; i++) {
         float totalRoll = 0;
         int i_min = Math.max(i - maxRadius, 0);
         int i_max = Math.min(i + maxRadius + 1, yCopy.length);
         float timeDiff = (float) (timeInMillis[i_max - 1] - timeInMillis[i_min]) / 1000;
         float maxDepth = Math.min(Math.max(bottom[i], 1000), 200); //Minimum of bottom depth and 200 m

         y[i] = Max.of(yCopy, Math.max(i - maxRadius, 0), Math.min(i + maxRadius + 1, yCopy.length));
         for (int j = i_min; j < i_max - 1; j++) {
            totalRoll += Math.abs(yCopy[j] - yCopy[j+1]);
         }
         // How many degrees roll until 200 m (or bottom) averaged over time (approx 2 * maxRadius seconds)
         //y[i] = totalRoll / (2 * maxRadius) * (2 * maxDepth / soundSpeed);
         y[i] = totalRoll / timeDiff * (2 * maxDepth / soundSpeed);
      }
      return y;
   }

   private static float[] postprocess2(float[] y, long[] time, float[] bottom) {
      return postprocess1(y, time, bottom);
   }
}
