package no.imr.lsss.region.ek500;

import no.imr.korona.Korona;
import no.imr.korona.data.formats.ek500.EK500SegmentHandle;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.resources.KoronaResource;
import no.imr.tools.Utils;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.List;

final class EK500WorkConversionGUIMain {
   private EK500WorkConversionGUIMain() {
   }

   public static void main(String[] args) {
      Utils.init(args, KoronaResource.KORONA_64);
      Path ek500Dir = Path.of(args[0]);
      Path ek60Dir = Path.of(args[1]);
      SwingUtilities.invokeLater(() -> start(ek500Dir, ek60Dir));
   }

   private static void start(Path ek500Dir, Path ek60Dir) {
      Korona korona = new Korona();
      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandlesInDirectory(ek500Dir);
      new EK500WorkConversionGUI(null,
            Utils.getAllOfType(segmentHandles, EK500SegmentHandle.class).toList(),
            ek500Dir,
            ek60Dir,
            List.of()
      );
   }
}
