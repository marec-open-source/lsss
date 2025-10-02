package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws latitude and longitude lines.
 */
public final class LatLonOverlay extends BaseMapOverlay {
   private final IntParameter pixelsBetweenLines = new IntParameter(
         new Name("PixelsBetweenLines", "Pixels between lines"),
         100, Unit.COUNT, ValueConstraints.gte(1),
         "Approximate distance in pixels between lines");

   private final FloatParameter fontSize = new FloatParameter(
         new Name("FontSize", "Font size"),
         14, Unit.PT, ValueConstraints.gte(1f),
         "Font size");

   private volatile @Nullable Font font;

   public LatLonOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            pixelsBetweenLines,
            fontSize
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      Listener recomputeListener = createRecomputeListener();
      registry.add(fontSize, newCoalescingExecListener(() -> font = null));
      registry.add(getParameters(), recomputeListener);
      registry.add(getMapModule().getGeographicalAreaChangeManager(), recomputeListener);
   }

   @Override
   protected void onDisable() {
      font = null;
   }

   @Override
   protected OverlayDisplayData recomputeDisplayData() {
      Rectangle2D geoRect = getMapModule().getGeoRect();
      double minLon = Math.max(-180, geoRect.getMinX());
      double minLat = Math.max(-90, geoRect.getMinY());
      double maxLon = Math.min(180, geoRect.getMaxX());
      double maxLat = Math.min(90, geoRect.getMaxY());
      Rectangle2D clampedGeoRect = new Rectangle2D.Double(minLon, minLat, maxLon - minLon, maxLat - minLat);

      double minX = longitudeToX(clampedGeoRect.getMinX(), geoRect);
      double minY = latitudeToY(clampedGeoRect.getMaxY(), geoRect);
      double dx = getWidth() * clampedGeoRect.getWidth() / geoRect.getWidth() - 1;
      double dy = getHeight() * clampedGeoRect.getHeight() / geoRect.getHeight() - 1;
      Rectangle2D clampedPixRect = new Rectangle2D.Double(minX, minY, dx, dy);

      Path2D.Float path = new Path2D.Float();
      List<GuiText> texts = new ArrayList<>();

      addLongitudes(geoRect, clampedGeoRect, clampedPixRect, path, texts);
      addLatitudes(geoRect, clampedGeoRect, clampedPixRect, path, texts);

      path.trimToSize();

      return new DisplayData(path, List.copyOf(texts));
   }

   private void addLongitudes(Rectangle2D geoRect, Rectangle2D clampedGeoRect, Rectangle2D clampedPixRect, Path2D.Float path, List<GuiText> texts) {
      double longitudeLineCount = Math.max(2, clampedPixRect.getWidth() / pixelsBetweenLines.getIntValue());
      double longitudeInterval = NiceNumber.niceDegree(clampedGeoRect.getWidth() / longitudeLineCount);
      double minLongitude = longitudeInterval * Math.ceil(clampedGeoRect.getMinX() / longitudeInterval);

      float minY = (float) clampedPixRect.getMinY();
      float maxY = (float) clampedPixRect.getMaxY();

      String format = Utils.getPrecisionString(longitudeInterval);

      int n = (int) Math.ceil(clampedGeoRect.getWidth() / longitudeInterval);
      for (int i = 0; i < n; i++) {
         double lon = minLongitude + i * longitudeInterval;
         float x = (float) longitudeToX(lon, geoRect);
         path.moveTo(x, minY);
         path.lineTo(x, maxY);

         texts.add(new GuiText(Utils.format(format, Math.abs(lon)) + '°' + (lon >= 0 ? 'E' : 'W'), Color.BLACK, x, minY,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.TOP, null));
      }
   }

   private void addLatitudes(Rectangle2D geoRect, Rectangle2D clampedGeoRect, Rectangle2D clampedPixRect, Path2D.Float path, List<GuiText> texts) {
      double latitudeLineCount = Math.max(2, clampedPixRect.getHeight() / pixelsBetweenLines.getIntValue());
      double latitudeInterval = NiceNumber.niceDegree(clampedGeoRect.getHeight() / latitudeLineCount);
      double minLatitude = latitudeInterval * Math.ceil(clampedGeoRect.getMinY() / latitudeInterval);

      float minX = (float) clampedPixRect.getMinX();
      float maxX = (float) clampedPixRect.getMaxX();

      String format = Utils.getPrecisionString(latitudeInterval);

      int n = (int) Math.ceil(clampedGeoRect.getHeight() / latitudeInterval);
      for (int i = 0; i < n; i++) {
         double lat = minLatitude + i * latitudeInterval;
         float y = (float) latitudeToY(lat, geoRect);
         path.moveTo(minX, y);
         path.lineTo(maxX, y);

         texts.add(new GuiText(Utils.format(format, Math.abs(lat)) + '°' + (lat >= 0 ? 'N' : 'S'), Color.BLACK, minX + 2, y,
               GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.CENTER, null));
      }
   }

   private double longitudeToX(double longitude, Rectangle2D geoRect) {
      return getWidth() * (longitude - geoRect.getMinX()) / geoRect.getWidth();
   }

   private double latitudeToY(double latitude, Rectangle2D geoRect) {
      return getHeight() * (geoRect.getMaxY() - latitude) / geoRect.getHeight();
   }

   private final class DisplayData extends OverlayDisplayData {
      private final Path2D.Float path;
      private final List<GuiText> texts;

      private DisplayData(Path2D.Float path, List<GuiText> texts) {
         this.path = path;
         this.texts = texts;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.GRAY);
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.draw(path);
      }

      @Override
      public void drawText(Graphics2D g2d) {
         Font font = LatLonOverlay.this.font;
         if (font == null) {
            font = g2d.getFont().deriveFont(Font.PLAIN, fontSize.getFloatValue());
            LatLonOverlay.this.font = font;
         }
         Font previousFont = g2d.getFont();
         g2d.setFont(font);
         GuiUtils.draw(g2d, texts);
         g2d.setFont(previousFont);
      }
   }
}
