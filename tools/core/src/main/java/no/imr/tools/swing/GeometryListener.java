package no.imr.tools.swing;

import no.imr.tools.logging.Log;
import no.imr.tools.parameter.StringParameter;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.Preferences;

/**
 * For persisting window geometry.
 */
public final class GeometryListener {
   private GeometryListener() {
   }

   public static void startParameterSyncing(Window window, @Nullable Dimension defaultSize, @Nullable Point defaultLocation, StringParameter parameter) {
      startPreferenceSyncing(window, parameter.getValue(), defaultSize, defaultLocation, parameter::setValue);
   }

   public static void startPreferenceSyncing(Window window, @Nullable Dimension defaultSize, @Nullable Point defaultLocation, Preferences preferences, String key) {
      startPreferenceSyncing(window, preferences.get(key, ""), defaultSize, defaultLocation, value -> preferences.put(key, value));
   }

   private static void startPreferenceSyncing(Window window, String initialGeometry, @Nullable Dimension defaultSize, @Nullable Point defaultLocation, Consumer<String> geometrySaver) {
      applyGeometryString(window, initialGeometry, defaultSize, defaultLocation);
      window.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            saveGeometryString(window, geometrySaver);
         }

         @Override
         public void componentMoved(ComponentEvent e) {
            saveGeometryString(window, geometrySaver);
         }
      });
   }

   private static void saveGeometryString(Window window, Consumer<String> geometrySaver) {
      String geometryString = window.getX() + " " + window.getY() + " " + window.getWidth() + " " + window.getHeight();
      if (window instanceof Frame frame) {
         String max = "";
         if ((frame.getExtendedState() & Frame.MAXIMIZED_HORIZ) != 0) {
            max += "h";
         }
         if ((frame.getExtendedState() & Frame.MAXIMIZED_VERT) != 0) {
            max += "v";
         }
         if (!max.isEmpty()) {
            geometryString += " " + max;
         }
      }

      if (!GuiUtils.isFullScreen(window)) {
         geometrySaver.accept(geometryString);
      }
   }

   private static void applyGeometryString(Window window, String geometry, @Nullable Dimension defaultSize, @Nullable Point defaultLocation) {
      try {
         String[] xywh = geometry.split("\\s+");
         int x = Integer.parseInt(xywh[0]);
         int y = Integer.parseInt(xywh[1]);
         int width = Integer.parseInt(xywh[2]);
         int height = Integer.parseInt(xywh[3]);

         GuiUtils.clampToScreen(window, x, y, width, height);

         if (window instanceof Frame frame && xywh.length >= 5) {
            String max = xywh[4];
            int extendedState = 0;
            if (max.contains("h")) {
               extendedState |= Frame.MAXIMIZED_HORIZ;
            }
            if (max.contains("v")) {
               extendedState |= Frame.MAXIMIZED_VERT;
            }
            frame.setExtendedState(extendedState);
         }
      } catch (Exception e) {
         if (!geometry.isBlank()) {
            Log.global.log(Level.WARNING, "Could not set window geometry: " + geometry, e);
         }

         if (defaultSize != null) {
            window.setSize(defaultSize);
         } else {
            window.pack();
         }

         if (defaultLocation != null) {
            window.setLocation(defaultLocation);
         } else {
            window.setLocationRelativeTo(null);
         }
      }
   }
}
