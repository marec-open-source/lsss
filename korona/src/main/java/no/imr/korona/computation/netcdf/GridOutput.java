package no.imr.korona.computation.netcdf;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;

import java.io.IOException;

abstract class GridOutput {
   GridOutput() {
   }

   abstract void addVariables(Group.Builder builder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim);

   abstract GridOutputWriter createWriter(NcGridWriter ncGridWriter) throws InvalidRangeException, IOException;
}
