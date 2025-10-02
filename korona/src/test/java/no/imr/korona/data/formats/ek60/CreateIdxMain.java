package no.imr.korona.data.formats.ek60;

import no.imr.korona.Korona;
import no.imr.korona.data.ping.PingIndexCorrectionOptions;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.test.BaseFileMain;

import java.nio.file.Path;
import java.util.List;

final class CreateIdxMain extends BaseFileMain {
   private CreateIdxMain() {
   }

   @Override
   protected void execute(Path file) {
      Korona korona = new Korona();
      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandlesInDirectory(file.getParent());
      new CreateMissingIdxGUI(korona.getDatagramTypeManager(), segmentHandles, new PingIndexCorrectionOptions(), false).start(null);
   }

   public static void main(String[] args) {
      new CreateIdxMain().start();
   }
}
