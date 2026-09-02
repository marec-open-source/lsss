package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.impl.StationaryPositionFunction;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.ts.TSData;
import no.imr.lsss.modules.ts.TSModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

public final class TsRelativeMainFrequencyFunction extends PingFunction {
   private final ToDoubleFunction<TSData> function;
   private final LSSS lsss;
   private @Nullable TSModule tsModule;

   private TsRelativeMainFrequencyFunction(Name name, Unit unit, ExportTransform exportTransform, LSSS lsss, ToDoubleFunction<TSData> function) {
      super(name, unit, exportTransform, true);

      this.function = function;
      this.lsss = lsss;
   }

   @Override
   public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
      tsModule = lsss.getModuleManager().getModule(TSModule.class);
      listenerRegistry.add(getChangeManager(), List.of(
            tsModule.getTSDetectionChangeManager(),
            lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency,
            lsss.getRegionManager().selectedRegions()
      ));
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      float mainFrequency = lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue();
      int mainChannel = dataFileSet.firstChannelClosestTo(mainFrequency);
      if (mainChannel < 1) {
         return Double.NaN;
      }
      if (tsModule == null) {
         return Double.NaN;
      }
      List<Region> regions = lsss.getRegionManager().selectedRegions().getValue();
      TSData mainTsData = strongestTsData(tsModule, regions, ping.getPingIndex(), mainChannel);
      if (mainTsData == null) {
         return Double.NaN;
      }
      TSData tsData = strongestTsData(tsModule, regions, ping.getPingIndex(), channel);
      if (tsData == null) {
         return Double.NaN;
      }
      return function.applyAsDouble(tsData) - function.applyAsDouble(mainTsData);
   }

   private static @Nullable TSData strongestTsData(TSModule tsModule, List<Region> regions, PingIndex pingIndex, int channel) {
      return regions.stream()
            .flatMap(region -> tsModule.getTSData(region, pingIndex, channel).stream())
            .max(Comparator.comparingDouble(TSData::tsc))
            .orElse(null);
   }

   public static PingFunction alongshipAngle(LSSS lsss) {
      return new TsRelativeMainFrequencyFunction(
            new Name("tsAlongshipAngleRelativeMainFrequency", "Strongest target alongship angle (rel. main freq.)"),
            Unit.DEGREES, ExportRounding.degrees(), lsss, TSData::alongshipAngle);
   }

   public static PingFunction athwartshipAngle(LSSS lsss) {
      return new TsRelativeMainFrequencyFunction(
            new Name("tsAthwartshipAngleRelativeMainFrequency", "Strongest target athwartship angle (rel. main freq.)"),
            Unit.DEGREES, ExportRounding.degrees(), lsss, TSData::athwartshipAngle);
   }

   public static PingFunction depth(LSSS lsss) {
      return new TsRelativeMainFrequencyFunction(
            new Name("tsDepthRelativeMainFrequency", "Strongest target depth (rel. main freq.)"),
            Unit.METER, ExportRounding.depth(), lsss, TSData::depth);
   }

   public static PingFunction alongshipCoordinate(LSSS lsss) {
      return new TsRelativeMainFrequencyFunction(
            new Name("tsAlongshipCoordinateRelativeMainFrequency", "Strongest target alongship coordinate (rel. main freq.)"),
            Unit.METER, ExportRounding.depth(), lsss, tsData -> toPos(tsData).x());
   }

   public static PingFunction athwartshipCoordinate(LSSS lsss) {
      return new TsRelativeMainFrequencyFunction(
            new Name("tsAthwartshipCoordinateRelativeMainFrequency", "Strongest target athwartship coordinate (rel. main freq.)"),
            Unit.METER, ExportRounding.depth(), lsss, tsData -> toPos(tsData).y());
   }

   public static PingFunction verticalCoordinate(LSSS lsss) {
      return new TsRelativeMainFrequencyFunction(
            new Name("tsVerticalCoordinateRelativeMainFrequency", "Strongest target vertical coordinate (rel. main freq.)"),
            Unit.METER, ExportRounding.depth(), lsss, tsData -> toPos(tsData).z());
   }

   private static Vec3 toPos(TSData tsData) {
      Measurement measurement = new Measurement(
            tsData.range(),
            (float) Math.toRadians(tsData.alongshipAngle()),
            (float) Math.toRadians(tsData.athwartshipAngle()),
            tsData.tsc()
      );
      return StationaryPositionFunction.measurementToGlobalPosition(measurement);
   }

   public static PingFunction tsc(LSSS lsss) {
      return new TsRelativeMainFrequencyFunction(
            new Name("tsTscRelativeMainFrequency", "Strongest target TSC (rel. main freq.)"),
            Unit.DB, ExportRounding.db(), lsss, TSData::tsc);
   }

   public static PingFunction tsu(LSSS lsss) {
      return new TsRelativeMainFrequencyFunction(
            new Name("tsTsuRelativeMainFrequency", "Strongest target TSU (rel. main freq.)"),
            Unit.DB, ExportRounding.db(), lsss, TSData::tsu);
   }
}
