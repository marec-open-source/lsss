package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import ucar.nc2.Dimension;
import ucar.nc2.Group;

final class GridEmptyOutput extends GridOutput {
   GridEmptyOutput() {
   }

   @Override
   void addVariables(Group.Builder builder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
   }

   @Override
   GridOutputWriter createWriter(NcGridWriter ncGridWriter) {
      return new GridEmptyWriter();
   }

   private static final class GridEmptyWriter extends GridOutputWriter {
      private GridEmptyWriter() {
      }

      @Override
      void write(Ping ping, int pingTimeIndex) {
      }
   }
}
