package no.imr.korona.computation.netcdf;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Group;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;

@FunctionalInterface
interface PingByPingBuilder {
   PingByPingWriter createWriter(NetcdfFormatWriter writer, Group group) throws InvalidRangeException, IOException;
}
