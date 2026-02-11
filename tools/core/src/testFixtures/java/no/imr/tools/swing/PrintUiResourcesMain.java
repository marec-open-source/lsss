package no.imr.tools.swing;

import no.imr.tools.Utils;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@SuppressWarnings("PMD.SystemPrintln")
final class PrintUiResourcesMain {
   private PrintUiResourcesMain() {
   }

   private static void run() {
      NavigableMap<Object, Color> colors = new TreeMap<>(Comparator.comparing(Object::toString));
      NavigableMap<Object, Font> fonts = new TreeMap<>(Comparator.comparing(Object::toString));
      for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) { // forEach not overridden in MultiUIDefaults
         Object key = entry.getKey();
         Object value = entry.getValue();
         if (value instanceof UIDefaults.ActiveValue activeValue) {
            value = activeValue.createValue(UIManager.getDefaults());
         }
         switch (value) {
            case Color color -> colors.put(key, color);
            case Font font -> fonts.put(key, font);
            default -> {
            }
         }
      }
      colors.forEach((key, color) -> {
         System.out.println(Utils.format("%-50s %-10s %-60s %-60s", key, ColorUtils.colorToHex(color), color, UIManager.get(key)));
      });
      System.out.println();
      fonts.forEach((key, font) -> {
         System.out.println(Utils.format("%-50s %s", key, font));
      });
   }

   static void main() {
      SwingUtilities.invokeLater(PrintUiResourcesMain::run);
   }
}
