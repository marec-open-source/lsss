package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.tools.Utils;
import no.imr.tools.geo.Earth;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

public final class ScaleMapOverlay extends BaseMapOverlay {
   private @Nullable Font font;
   private boolean useNmi = true;

   public ScaleMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getMapModule().getGeographicalAreaChangeManager(), createRecomputeListener());
   }

   @Override
   protected OverlayDisplayData recomputeDisplayData() {
      double latitude = getMapModule().getGeoCenter().getLatitude();
      double radius = Earth.getRadius(latitude) * Math.cos(Math.toRadians(latitude));
      double distance = radius * Math.toRadians(getMapModule().getLongitudeExtent());
      if (useNmi) {
         distance = Utils.meterToNmi(distance);
      }
      double distancePerPixel = distance / getWidth();

      double rulerPixels = 50;
      double rulerDistance = NiceNumber.niceNumber(rulerPixels * distancePerPixel, false);
      rulerPixels = rulerDistance / distancePerPixel;

      float x1 = getWidth() - 10;
      float x0 = (float) (x1 - rulerPixels);
      float y = getHeight() - 20;

      Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, 4);
      path.moveTo(x0, y - 3);
      path.lineTo(x0, y);
      path.lineTo(x1, y);
      path.lineTo(x1, y - 3);

      String string = useNmi ? getNmiText(rulerDistance) : getMeterText(rulerDistance);
      GuiText text = new GuiText(string, Color.BLACK, (x0 + x1) / 2, y + 1,
            GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.TOP, null);

      return new DisplayData(path, text);
   }

   private static String getNmiText(double nmi) {
      if (nmi >= 1) {
         return Math.round(nmi) + " nmi";
      } else {
         return nmi + " nmi";
      }
   }

   private static String getMeterText(double meters) {
      if (meters >= 1000) {
         return Math.round(meters / 1000) + " km";
      } else if (meters >= 1) {
         return Math.round(meters) + " m";
      } else {
         return meters + " m";
      }
   }

   private final class DisplayData extends OverlayDisplayData {
      private final Path2D.Float path;
      private final GuiText text;

      private DisplayData(Path2D.Float path, GuiText text) {
         this.path = path;
         this.text = text;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.BLACK);
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.draw(path);
      }

      @Override
      public void drawText(Graphics2D g2d) {
         if (font == null) {
            font = g2d.getFont().deriveFont(Font.PLAIN);
         }
         Font previousFont = g2d.getFont();
         g2d.setFont(font);
         text.draw(g2d);
         g2d.setFont(previousFont);
      }
   }
}
