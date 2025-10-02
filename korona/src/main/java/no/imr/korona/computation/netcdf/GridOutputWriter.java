package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

abstract class GridOutputWriter {
   GridOutputWriter() {
   }

   abstract void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException;
}
