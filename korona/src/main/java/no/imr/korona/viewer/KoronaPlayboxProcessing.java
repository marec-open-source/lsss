package no.imr.korona.viewer;

import no.imr.korona.computation.ModuleContainerComputation;

import java.io.IOException;

record KoronaPlayboxProcessing(
      ModuleContainerComputation computation,
      TmpFileWriter tmpFileWriter,
      DisplayRunner displayRunner
) {
   void close() throws IOException {
      displayRunner.stop();
      computation.close();
      tmpFileWriter.close();
   }
}
