package no.imr.korona.computation.categorization;

import no.imr.korona.Korona;
import no.imr.korona.computation.feature.CategoryVisualizer;
import no.imr.korona.resources.KoronaResource;
import no.imr.korona.test.KoronaTestUtils;
import no.imr.korona.viewer.KoronaHelpSystem;
import no.imr.tools.Utils;

import javax.swing.SwingUtilities;
import java.awt.Dialog;

final class CategoryVisualizerMain {
   private CategoryVisualizerMain() {
   }

   public static void main(String[] args) {
      Utils.init(args, KoronaResource.KORONA_64);
      SwingUtilities.invokeLater(CategoryVisualizerMain::start);
   }

   private static void start() {
      Korona korona = new Korona();
      KoronaHelpSystem.createHelpSystem(korona);
      new CategoryVisualizer(KoronaTestUtils.createConfigurator(korona), null, Dialog.ModalityType.DOCUMENT_MODAL);
   }
}
