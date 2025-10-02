package no.imr.korona.viewer.overlays;

import no.imr.tools.range.FloatRange;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;

/**
 * Displays depth markers.
 */
public final class EchogramDepthOverlay extends EchogramOverlay {
   private DepthMarkerData depthMarkerData = new DepthMarkerData();
   private boolean needUpdate = true;

   public EchogramDepthOverlay() {
   }

   @Override
   public void resized(GraphicsConfiguration graphicsConfiguration, int width, int height) {
      needUpdate();
   }

   @Override
   public void depthRangeChanged() {
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
      return getEchogramDisplay().getFonts().getNormal();
   }

   private void updateGraphics(Graphics2D g2d) {
      FloatRange depthRange = getEchogramDisplay().getDepthRange();
      FontMetrics fontMetrics = g2d.getFontMetrics(getFont());
      depthMarkerData = new DepthMarkerData(depthRange, getWidth(), getHeight(), DepthMarkerData.PIXELS_PER_MARKER, fontMetrics, false);
   }

   @Override
   public void refresh() {
      needUpdate();
   }
}
