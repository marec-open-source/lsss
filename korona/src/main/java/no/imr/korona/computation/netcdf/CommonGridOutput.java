package no.imr.korona.computation.netcdf;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;

import java.io.IOException;

abstract class CommonGridOutput {
   CommonGridOutput() {
   }

   abstract void addVariables(Group.Builder builder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim);

   abstract CommonGridOutputWriter createWriter(NcGridWriter ncGridWriter) throws InvalidRangeException, IOException;
}
