package no.imr.korona.data.datagrams.subdatagrams.plot;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;

public final class PlotParameterUtils {
   private PlotParameterUtils() {
   }

   public static PlotParameterConfigSubDatagram getOrCreateConfigSubDatagram(PingConfiguration pingConfiguration) {
      PlotParameterConfigSubDatagram configSubDatagram = pingConfiguration.getConfigurationItem(PlotParameterConfigSubDatagram.class);
      if (configSubDatagram == null) {
         configSubDatagram = new PlotParameterConfigSubDatagram(pingConfiguration.getRawFileConfiguration().getNTDate());
         pingConfiguration.getConfigurationItems().add(configSubDatagram);
      }
      return configSubDatagram;
   }

   public static PlotParameterValueSubDatagram getOrCreateValueSubDatagram(Ping ping) {
      PlotParameterValueSubDatagram valueSubDatagram = ping.getPingItem(PlotParameterValueSubDatagram.class);
      if (valueSubDatagram == null) {
         valueSubDatagram = new PlotParameterValueSubDatagram(ping.getNTDate());
         ping.add(valueSubDatagram);
      }
      return valueSubDatagram;
   }
}
