package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.lsss.LSSS;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.ParameterExport;
import no.marec.lsss.api.modules.EchogramPlotFunction;
import no.marec.lsss.api.util.observing.ObservableProperty;
import org.jfree.chart.plot.XYPlot;

import java.time.Instant;

public abstract class PingFunction implements EchogramPlotFunction {
   private final Name name;
   private final Unit unit;
   private final ExportTransform exportTransform;
   private final boolean channelDependent;
   private final ChangeManager changeManager = new ChangeManager();
   public final BooleanParameter selected;

   protected PingFunction(Name name, Unit unit, ExportTransform exportTransform, boolean channelDependent, String description) {
      this.name = name;
      this.unit = unit;
      this.exportTransform = exportTransform;
      this.channelDependent = channelDependent;
      selected = new BooleanParameter(name, false, unit, description);
   }

   protected PingFunction(Name name, Unit unit, ExportTransform exportTransform, boolean channelDependent) {
      this(name, unit, exportTransform, channelDependent, "");
   }

   protected PingFunction(Name name, Unit unit, ExportTransform exportTransform) {
      this(name, unit, exportTransform, false);
   }

   public Name getName() {
      return name;
   }

   @Override
   public String getId() {
      return getPersistentName();
   }

   public String getPersistentName() {
      return name.persistentName();
   }

   public Unit getUnit() {
      return unit;
   }

   public ParameterExport getParameterExport() {
      return new ParameterExport(name.persistentName(), unit, exportTransform);
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public boolean isChannelDependent() {
      return channelDependent;
   }

   public abstract double compute(DataFileSet dataFileSet, Ping ping, int channel);

   public float[] postprocess(float[] y, Instant[] instants, float[] bottom) {
      return y;
   }

   public void addMarkers(XYPlot plot) {
   }

   public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
   }

   @Override
   public ObservableProperty<Boolean> selected() {
      return selected;
   }
}
