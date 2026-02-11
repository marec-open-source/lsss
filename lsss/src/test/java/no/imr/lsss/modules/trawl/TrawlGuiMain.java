package no.imr.lsss.modules.trawl;

import no.imr.tools.swing.GeometryListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.prefs.Preferences;

final class TrawlGuiMain {
   private final JFrame frame = new JFrame();
   private final TrawlGui trawlGui = new TrawlGui();

   private TrawlGuiMain() {
      Preferences preferences = Preferences.userRoot().node("no/imr/lsss/modules/trawl");
      trawlGui.setUseEnglish(false);
      String lastFile = preferences.get("directory", "");
      trawlGui.setFile(lastFile.isBlank() ? null : Path.of(lastFile));
      trawlGui.getFileChangeManager().addListener(() -> {
         Path file = trawlGui.getFile();
         preferences.put("directory", file != null ? file.toString() : "");
         updateTitle();
      });
      updateTitle();

      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(trawlGui.getComponent());
      GeometryListener.startPreferenceSyncing(frame, new Dimension(1100, 900), null, preferences, "frameGeometry");
      frame.setVisible(true);
   }

   private void updateTitle() {
      String title = TrawlGui.class.getSimpleName();
      Path file = trawlGui.getFile();
      if (file != null) {
         title = title + " - " + file;
      }
      frame.setTitle(title);
   }

   static void main() {
      SwingUtilities.invokeLater(TrawlGuiMain::new);
   }
}
