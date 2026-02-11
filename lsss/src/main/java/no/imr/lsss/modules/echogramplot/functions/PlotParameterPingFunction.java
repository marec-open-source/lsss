package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datagrams.subdatagrams.plot.PlotParameterValueSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.plot.pojo.PlotParameterConfig;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.util.PingIndexConverter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PlotParameterPingFunction {
   private PlotParameterPingFunction() {
   }

   public static PingFunction of(PlotParameterConfig config) {
      Name name = new Name(config.id, config.label);
      if (config.perChannel) {
         return new PerChannel(name, new Unit(config.unit));
      } else {
         return new PerPing(name, new Unit(config.unit));
      }
   }

   public static PingFunction ofOtherData(PlotParameterConfig config, DataFileSet dataFileSet, PingIndexConverter pingIndexConverter) {
      Name name = new Name(config.id, config.label);
      return new OtherDataPerPing(name, new Unit(config.unit), dataFileSet, pingIndexConverter);
   }

   private static double getPerPingValue(Ping ping, String id) {
      return ping.getPingItems(PlotParameterValueSubDatagram.class)
            .map(subDatagram -> subDatagram.getPerPingParameterValues().get(id))
            .filter(Objects::nonNull)
            .mapToDouble(Float::doubleValue)
            .findFirst()
            .orElse(Double.NaN);
   }

   private static double getPerChannelValue(Ping ping, int channel, String id) {
      return ping.getPingItems(PlotParameterValueSubDatagram.class)
            .map(subDatagram -> subDatagram.getPerChannelValues().get(id))
            .filter(Objects::nonNull)
            .map(map -> map.get(channel))
            .filter(Objects::nonNull)
            .mapToDouble(Float::doubleValue)
            .findFirst()
            .orElse(Double.NaN);
   }

   private static final class PerPing extends PingFunction {
      private PerPing(Name name, Unit unit) {
         super(name, unit, ExportTransform.identity());
      }

      @Override
      public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
         return getPerPingValue(ping, getPersistentName());
      }
   }

   private static final class PerChannel extends PingFunction {
      private PerChannel(Name name, Unit unit) {
         super(name, unit, ExportTransform.identity(), true);
      }

      @Override
      public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
         return getPerChannelValue(ping, channel, getPersistentName());
      }
   }

   private static final class OtherDataPerPing extends PingFunction {
      private final DataFileSet otherDataFileSet;
      private final PingIndexConverter pingIndexConverter;
      private final Map<PingIndex, Float> cache = new HashMap<>();

      private OtherDataPerPing(Name name, Unit unit, DataFileSet otherDataFileSet, PingIndexConverter pingIndexConverter) {
         super(name, unit, ExportTransform.identity());

         this.otherDataFileSet = otherDataFileSet;
         this.pingIndexConverter = pingIndexConverter;
      }

      @Override
      public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
         PingIndex otherPingIndex = pingIndexConverter.lsssToContainingOther(ping.getPingIndex());
         if (otherPingIndex == null) {
            return Double.NaN;
         }
         return cache.computeIfAbsent(otherPingIndex, _ -> {
            Ping otherPing = otherDataFileSet.getPing(otherPingIndex);
            return (float) getPerPingValue(otherPing, getPersistentName());
         });
      }
   }
}
