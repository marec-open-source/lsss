package no.imr.lsss.modules.echogram;

import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;

import java.io.IOException;
import java.util.List;

public final class PelagicEchogramExporter extends EchogramExporter {
   private final BooleanParameter zoomedVertically = new BooleanParameter(
         new Name("ZoomedVertically", "Zoomed vertically"),
         false,
         "Only raw datagrams from vertically zoomed range are exported");

   public PelagicEchogramExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("EchosounderData", "Echosounder data"), "Visible echosounder data");
   }

   @Override
   protected List<? extends BaseParameter<?>> getSettingsParameters() {
      return List.of(zoomedVertically);
   }

   @Override
   protected void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      exportEchogram(getLSSS().getInterpretationSettings().getDataFileSet(), getLSSS().getInterpretationSettings().getPingRange(), asyncHandle, progressHandler);
   }

   @Override
   protected Ping filterPing(Ping ping) {
      if (zoomedVertically.getBooleanValue()) {
         FloatRange depthRange = getLSSS().getInterpretationSettings().getPelagicZSettings().getDepthRange(ping.getPingIndex());
         return zoomVertically(ping, depthRange);
      }
      return ping;
   }

   private static Ping zoomVertically(Ping ping, FloatRange depthRange) {
      DefaultPing zoomedPing = new DefaultPing(ping.getPingConfiguration(), ping.getPingIndex(), ping.getBot0Datagram());
      ping.getNonNullPowerDatas().forEach(powerData -> {
         zoomedPing.add(powerData.makeCopyOfDepthRange(depthRange));
      });
      return zoomedPing;
   }
}
