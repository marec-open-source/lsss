package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

interface NcPingWriter extends AutoCloseable {
   void writePing(Ping ping) throws InvalidRangeException, IOException;

   @Override
   void close() throws IOException;
}
