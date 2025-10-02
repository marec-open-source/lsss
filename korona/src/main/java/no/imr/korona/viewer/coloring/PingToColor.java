package no.imr.korona.viewer.coloring;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.range.FloatRange;

import java.util.List;

@FunctionalInterface
public interface PingToColor {
   void convertToColor(Ping ping, int channel, int[] rgbs, FloatRange depthRange);

   static PingToColor multi(List<PingToColor> pingToColors) {
      return (ping, channel, rgbs, depthRange) -> {
         pingToColors.forEach(pingToColor -> pingToColor.convertToColor(ping, channel, rgbs, depthRange));
      };
   }
}
