package no.imr.korona.computation.netcdf;

import ucar.nc2.Dimension;
import ucar.nc2.Group;

interface CommonGridOutput {
   PingByPingBuilder createBuilder(NcConfig ncConfig, RangeConfig rangeConfig, Group.Builder groupBuilder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim);
}
