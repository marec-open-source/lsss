package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.util.GeoPoint;

public final class SimplePingFunction extends PingFunction {
   private final SimpleComputeFunction function;

   private SimplePingFunction(Name name, Unit unit, ExportTransform exportTransform, boolean channelDependent, SimpleComputeFunction function) {
      super(name, unit, exportTransform, channelDependent);

      this.function = function;
   }

   private SimplePingFunction(Name name, Unit unit, ExportTransform exportTransform, SimpleComputeFunction function) {
      this(name, unit, exportTransform, false, function);
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      return function.compute(dataFileSet, ping, channel);
   }

   public static PingFunction bottomDepth() {
      return new SimplePingFunction(new Name("bottomDepth", "Bottom depth"), Unit.METER, ExportRounding.depth(), true,
            (_, ping, channel) -> {
               double bottomDepth = ping.getBot0Datagram().getChannelDepths()[channel - 1];
               return bottomDepth == 0 ? Double.NaN : bottomDepth;
            });
   }

   public static PingFunction bottomDepthRelativeMainFrequency(LSSS lsss) {
      return new SimplePingFunction(new Name("bottomDepthRelativeMainFrequency", "Bottom depth (rel. main freq.)"), Unit.METER, ExportRounding.depth(), true,
            (dataFileSet, ping, channel) -> {
               float mainFrequency = lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue();
               int mainChannel = dataFileSet.firstChannelClosestTo(mainFrequency);
               if (mainChannel < 1) {
                  return Double.NaN;
               }
               double bottomDepth = ping.getBot0Datagram().getChannelDepths()[channel - 1];
               double refBottomDepth = ping.getBot0Datagram().getChannelDepths()[mainChannel - 1];
               return bottomDepth == 0 || refBottomDepth == 0 ? Double.NaN : bottomDepth - refBottomDepth;
            });
   }

   public static PingFunction timeBetweenPings() {
      return new SimplePingFunction(new Name("timeBetweenPings", "Time between pings"), Unit.SECONDS, ExportTransform.round(100),
            (dataFileSet, ping, _) -> {
               PingIndex nextPingIndex = dataFileSet.getPingIndex(ping.getPingNumber() + 1);
               return (nextPingIndex.getNTDate() - ping.getNTDate()) / (double) NTDate.UNITS_PER_SECOND;
            });
   }

   public static PingFunction distanceBetweenPings() {
      return new SimplePingFunction(new Name("distanceBetweenPings", "Distance between pings"), Unit.METER, ExportTransform.round(1000),
            (dataFileSet, ping, _) -> {
               PingIndex nextPingIndex = dataFileSet.getPingIndex(ping.getPingNumber() + 1);
               return Utils.nmiToMeter(nextPingIndex.getVesselDistance() - ping.getVesselDistance());
            });
   }

   public static PingFunction vesselDistance() {
      return new SimplePingFunction(new Name("vesselDistance", "Vessel distance"), Unit.NAUTICAL_MILES, ExportRounding.vesselDistance(),
            (_, ping, _) -> ping.getVesselDistance());
   }

   public static PingFunction vesselSpeed() {
      return new SimplePingFunction(new Name("vesselSpeed", "Vessel speed"), Unit.KNOTS, ExportTransform.round(100),
            (dataFileSet, ping, _) -> DataUtils.getKnots(ping, dataFileSet));
   }

   public static PingFunction longitude() {
      return new SimplePingFunction(new Name("longitude", "Longitude"), Unit.DEGREES, ExportRounding.geoPos(),
            (_, ping, _) -> {
               GeoPoint geoPos = ping.getPingIndex().getGeographicalPosition();
               return geoPos != null ? geoPos.getLongitude() : Double.NaN;
            });
   }

   public static PingFunction latitude() {
      return new SimplePingFunction(new Name("latitude", "Latitude"), Unit.DEGREES, ExportRounding.geoPos(),
            (_, ping, _) -> {
               GeoPoint geoPos = ping.getPingIndex().getGeographicalPosition();
               return geoPos != null ? geoPos.getLatitude() : Double.NaN;
            });
   }

   public static PingFunction heading() {
      return new SimplePingFunction(new Name("heading", "Heading"), Unit.DEGREES, ExportRounding.degrees(),
            (_, ping, _) -> DataUtils.getHeadingFromNmea(ping).orElse(Double.NaN));
   }

   @FunctionalInterface
   private interface SimpleComputeFunction {
      double compute(DataFileSet dataFileSet, Ping ping, int channel);
   }
}
