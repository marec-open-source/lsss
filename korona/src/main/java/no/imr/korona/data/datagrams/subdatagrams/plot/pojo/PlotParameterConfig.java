package no.imr.korona.data.datagrams.subdatagrams.plot.pojo;

import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;

public final class PlotParameterConfig {
   public String id = "";
   public String label = "";
   public String unit = "";
   public boolean perChannel;

   public PlotParameterConfig() {
   }

   public PlotParameterConfig(Name name, Unit unit, boolean perChannel) {
      id = name.persistentName();
      label = name.displayName();
      this.unit = unit.text();
      this.perChannel = perChannel;
   }
}
