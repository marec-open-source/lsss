package no.imr.korona.computation.categorization.apriori.gui;

import no.imr.korona.computation.categorization.apriori.APrioriPolygon;
import no.imr.korona.computation.categorization.apriori.GeoAPriori;
import no.imr.korona.resources.KoronaResource;
import no.imr.korona.util.KoronaPreferences;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dialog;
import java.io.IOException;
import java.util.logging.Level;
import java.util.prefs.Preferences;

final class GeoAPrioriEditorMain {
   private GeoAPrioriEditorMain() {
   }

   public static void main(String[] args) {
      Utils.init(args, KoronaResource.KORONA_64);
      SwingUtilities.invokeLater(GeoAPrioriEditorMain::run);
   }

   private static void run() {
      Preferences preferences = KoronaPreferences.node("test");
      String key = "GeoAPrioriEditorMain";

      String xml = preferences.get(key, null);
      GeoAPriori geoAPriori = parseGeoAPriori(xml);

      GeoAPrioriEditor geoAPrioriEditor = new GeoAPrioriEditor(geoAPriori);
      JDialog dialog = new JDialog(null, GeoAPrioriEditorMain.class.getSimpleName(), Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.add(geoAPrioriEditor.getComponent());
      dialog.setSize(800, 800);
      dialog.setLocationRelativeTo(null);
      dialog.setVisible(true);

      preferences.put(key, XmlUtils.toDefaultString(geoAPriori.toXml()));
   }

   private static GeoAPriori parseGeoAPriori(@Nullable String xml) {
      if (xml != null) {
         try {
            return new GeoAPriori(XmlUtils.readDocument(xml).getRootElement());
         } catch (IOException e) {
            Log.global.log(Level.WARNING, e.getMessage(), e);
         }
      }
      return createDefaultGeoAPriori();
   }

   private static GeoAPriori createDefaultGeoAPriori() {
      GeoAPriori geoAPriori;
      geoAPriori = new GeoAPriori();
      APrioriPolygon p1 = new APrioriPolygon();
      geoAPriori.getPolygons().add(p1);
      p1.getGeoPoints().add(new GeoPoint(0, 50));
      p1.getGeoPoints().add(new GeoPoint(40, 70));
      p1.getGeoPoints().add(new GeoPoint(0, 70));
      p1.update();
      return geoAPriori;
   }
}
