package no.imr.korona.data.datagrams.subdatagrams.plot.pojo;

import java.util.HashMap;
import java.util.Map;

public final class PlotParameterValues {
   public Map<String, Float> perPing = new HashMap<>();
   public Map<String, Map<Integer, Float>> perChannel = new HashMap<>();

   public PlotParameterValues() {
   }
}
