package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.ExportRounding;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

import java.util.function.ToDoubleFunction;

public final class Nqp0DatagramFunction extends PingFunction {
   private final ToDoubleFunction<Nqp0Datagram> function;

   private Nqp0DatagramFunction(Name name, Unit unit, ExportTransform exportTransform, ToDoubleFunction<Nqp0Datagram> function) {
      super(name, unit, exportTransform, true);

      this.function = function;
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      return ping.getPingItems(Nqp0Datagram.class)
            .filter(nqp0Datagram -> nqp0Datagram.getChannel() == channel)
            .mapToDouble(function)
            .findFirst()
            .orElse(Double.NaN);
   }

   public static Nqp0DatagramFunction average() {
      return new Nqp0DatagramFunction(new Name("noiseAverage", "Noise: Average"), Unit.DB, ExportRounding.db(),
            nqp0Datagram -> PowerData.svToLogSv(nqp0Datagram.getAverage()));
   }

   public static Nqp0DatagramFunction upperLimit() {
      return new Nqp0DatagramFunction(new Name("noiseUpperLimit", "Noise: Upper limit"), Unit.DB, ExportRounding.db(),
            nqp0Datagram -> PowerData.svToLogSv(nqp0Datagram.getUpperLimit()));
   }

   public static Nqp0DatagramFunction quality() {
      return new Nqp0DatagramFunction(new Name("noiseQuality", "Noise: Quality"), Unit.DIMENSIONLESS, ExportTransform.round(100),
            Nqp0Datagram::getQuality);
   }
}
