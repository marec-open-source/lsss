package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.ComplexChannelData;
import no.imr.korona.util.ExportRounding;
import no.imr.tools.math.MathUtils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

import java.util.function.ToDoubleFunction;

public final class ComplexChannelDataParameterFunction extends PingFunction {
   private final ToDoubleFunction<ComplexChannelData> function;

   private ComplexChannelDataParameterFunction(Name name, Unit unit, ExportTransform exportTransform, ToDoubleFunction<ComplexChannelData> function) {
      super(name, unit, exportTransform, true);

      this.function = function;
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      ChannelData channelData = ping.getChannelData(channel);
      if (!(channelData instanceof ComplexChannelData complexChannelData)) {
         return Double.NaN;
      }
      return function.applyAsDouble(complexChannelData);
   }

   public static ComplexChannelDataParameterFunction slope() {
      Name name = new Name("complexSlope", "Complex: Slope");
      return new ComplexChannelDataParameterFunction(name, Unit.NONE, ExportTransform.round(1e5), ComplexChannelData::getSlope);
   }

   public static ComplexChannelDataParameterFunction transducerImpedance(int sectorIndex) {
      int sector = sectorIndex + 1;
      Name name = new Name("complexTransducerImpedanceSector" + sector, "Complex: Transducer impedance, sector " + sector);
      return new ComplexChannelDataParameterFunction(name, Unit.OHM, ExportTransform.round(1000), complexChannelData -> {
         if (sectorIndex >= complexChannelData.getSectorCount()) {
            return Double.NaN;
         }
         return complexChannelData.getTransducerImpedanceForSector(sectorIndex).abs();
      });
   }

   public static ComplexChannelDataParameterFunction transducerImpedancePhase(int sectorIndex) {
      int sector = sectorIndex + 1;
      Name name = new Name("complexTransducerImpedancePhaseSector" + sector, "Complex: Transducer impedance phase, sector " + sector);
      return new ComplexChannelDataParameterFunction(name, Unit.DEGREES, ExportRounding.degrees(), complexChannelData -> {
         if (sectorIndex >= complexChannelData.getSectorCount()) {
            return Double.NaN;
         }
         double arg = complexChannelData.getTransducerImpedanceForSector(sectorIndex).arg();
         return MathUtils.normalizeAngle0To360(Math.toDegrees(arg));
      });
   }
}
