package no.imr.korona.computation.netcdf;

import ucar.nc2.Group;

@FunctionalInterface
interface PingByPingOutput {
   PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig);
}
