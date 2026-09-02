package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
interface PingByPingWriter {
   void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException;

   static PingByPingWriter empty() {
      return (_, _) -> {
      };
   }

   static PingByPingWriter of(List<PingByPingWriter> writers) {
      return (ping, pingTimeIndex) -> {
         for (PingByPingWriter writer : writers) {
            writer.write(ping, pingTimeIndex);
         }
      };
   }
}
