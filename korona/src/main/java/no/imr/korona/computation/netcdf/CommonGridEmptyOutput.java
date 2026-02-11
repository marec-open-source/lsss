package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import ucar.nc2.Dimension;
import ucar.nc2.Group;

final class CommonGridEmptyOutput extends CommonGridOutput {
   CommonGridEmptyOutput() {
   }

   @Override
   void addVariables(Group.Builder builder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
   }

   @Override
   CommonGridOutputWriter createWriter(NcGridWriter ncGridWriter) {
      return new CommonGridEmptyWriter();
   }

   private static final class CommonGridEmptyWriter extends CommonGridOutputWriter {
      private CommonGridEmptyWriter() {
      }

      @Override
      void write(Ping ping, int pingTimeIndex) {
      }
   }
}
