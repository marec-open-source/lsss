package no.imr.korona.viewer.overlays;

import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.korona.viewer.Fonts;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.overlay.Overlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;

/**
 * Displays depth markers.
 */
public final class EchogramSettingsDepthOverlay extends Overlay {
   private final EchogramZSettings settings;
   private final Fonts fonts;
   private DepthMarkerData depthMarkerData = new DepthMarkerData();
   private boolean needUpdate = true;

   public EchogramSettingsDepthOverlay(EchogramZSettings settings, Fonts fonts) {
      this.settings = settings;
      this.fonts = fonts;
      settings.getZoomedChangeManager().addListener(this::needUpdate);
   }

   @Override
   public void resized(GraphicsConfiguration graphicsConfiguration, int width, int height) {
      needUpdate();
   }

   private void needUpdate() {
      needUpdate = true;
      repaint();
   }

   @Override
   public void draw(Graphics2D g2d) {
      if (needUpdate) {
         updateGraphics(g2d);
         needUpdate = false;
      }

      g2d.setStroke(DepthMarkerData.STROKE);
      g2d.setColor(Color.BLACK);
      depthMarkerData.draw(g2d);
   }

   @Override
   public void drawText(Graphics2D g2d) {
      Font previousFont = g2d.getFont();
      g2d.setFont(getFont());
      depthMarkerData.drawText(g2d);
      g2d.setFont(previousFont);
   }

   private Font getFont() {
      return fonts.getNormal();
   }

   private void updateGraphics(Graphics2D g2d) {
      FloatRange depthRange = settings.getZoomedZRange();
      FontMetrics fontMetrics = g2d.getFontMetrics(getFont());
      depthMarkerData = new DepthMarkerData(depthRange, getWidth(), getHeight(), DepthMarkerData.PIXELS_PER_MARKER, fontMetrics, false);
   }
}
