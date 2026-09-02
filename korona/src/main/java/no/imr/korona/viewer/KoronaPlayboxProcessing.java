package no.imr.korona.viewer;

import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.tools.Utils;

import java.io.IOException;

record KoronaPlayboxProcessing(
      ModuleContainerComputation computation,
      TmpFileWriter tmpFileWriter,
      DisplayRunner displayRunner
) {
   void close() throws IOException {
      Utils.closeAll(
            displayRunner::stop,
            computation::close,
            tmpFileWriter::close
      );
   }
}
