package no.imr.lsss.framework.extensions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.echogramplot.EchogramPlotModule;
import no.imr.lsss.modules.echogramplot.functions.PingFunction;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.marec.lsss.api.modules.EchogramPlot;
import no.marec.lsss.api.modules.EchogramPlotFunction;
import no.marec.lsss.api.modules.EchogramPlotFunctionEvaluation;
import org.jspecify.annotations.Nullable;

final class EchogramPlotImpl implements EchogramPlot {
   private final LSSS lsss;
   private @Nullable EchogramPlotModule echogramPlotModule;

   EchogramPlotImpl(LSSS lsss) {
      this.lsss = lsss;
   }

   private EchogramPlotModule getEchogramPlotModule() {
      EchogramPlotModule module = echogramPlotModule;
      if (module == null) {
         module = lsss.getModuleManager().getModule(EchogramPlotModule.class);
         echogramPlotModule = module;
      }
      return module;
   }

   @Override
   public EchogramPlotFunction addFunction(String id, String label, String description,
                                           String unit, boolean channelDependent, EchogramPlotFunctionEvaluation evaluation) {
      PingFunction f = new PingFunction(new Name(id, label), new Unit(unit), ExportTransform.identity(), channelDependent, description) {
         @Override
         public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
            return evaluation.evaluate(ping, channel);
         }
      };
      getEchogramPlotModule().add(f);
      return f;
   }

   @Override
   public void removeFunction(EchogramPlotFunction function) {
      if (function instanceof PingFunction f) {
         getEchogramPlotModule().remove(f);
      }
   }
}
