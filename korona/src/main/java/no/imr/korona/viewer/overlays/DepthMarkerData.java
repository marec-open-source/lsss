package no.imr.korona.viewer.overlays;

import no.imr.tools.Utils;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public final class DepthMarkerData {
   public static final BasicStroke STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{3, 5}, 0);
   public static final int PIXELS_PER_MARKER = 70;

   private final Path2D.Float path;
   private final List<GuiText> texts;

   public DepthMarkerData() {
      path = new Path2D.Float(Path2D.WIND_NON_ZERO, 0);
      texts = List.of();
   }

   public DepthMarkerData(FloatRange zRange, int width, int height, int pixelsPerMarker, FontMetrics fontMetrics, boolean addUnit) {
      path = new Path2D.Float();
      texts = new ArrayList<>();

      int approximateLineCount = Math.max(3, height / pixelsPerMarker);
      double dz = NiceNumber.niceNumber((double) zRange.getSize() / approximateLineCount, true);
      double z0 = dz * Math.ceil(zRange.min() / dz);
      double z1 = dz * Math.floor(zRange.max() / dz);
      int lineCount = (int) Math.round((z1 - z0) / dz);

      String format = Utils.getPrecisionString(dz);
      if (addUnit) {
         format += " m";
      }
      float xText = 5 + Math.max(fontMetrics.stringWidth(Utils.format(format, z0)), fontMetrics.stringWidth(Utils.format(format, z1)));
      float xPathMin = xText + 10;

      for (int i = 0; i <= lineCount; i++) {
         double z = z0 + i * dz;
         float y = height * zRange.valueToFraction((float) z);
         path.moveTo(xPathMin, y);
         path.lineTo(width, y);

         String text = Utils.format(format, z);
         texts.add(new GuiText(text, Color.BLACK, xText, y, GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.CENTER, null));
      }
   }

   public void draw(Graphics2D g2d) {
      g2d.draw(path);
   }

   public void drawText(Graphics2D g2d) {
      GuiUtils.draw(g2d, texts);
   }
}
