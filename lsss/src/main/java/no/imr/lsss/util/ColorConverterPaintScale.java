package no.imr.lsss.util;

import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import org.jfree.chart.renderer.PaintScale;

import java.awt.Color;
import java.awt.Paint;

public final class ColorConverterPaintScale implements PaintScale {
   private final SingleValueColorConverter colorConverter;

   public ColorConverterPaintScale(SingleValueColorConverter colorConverter) {
      this.colorConverter = colorConverter;
   }

   @Override
   public double getLowerBound() {
      return colorConverter.getContinuousVariable().getSettings().getRange().min();
   }

   @Override
   public double getUpperBound() {
      return colorConverter.getContinuousVariable().getSettings().getRange().max();
   }

   @Override
   public Paint getPaint(double value) {
      int rgb = colorConverter.getRGB((float) value);
      return new Color(rgb);
   }
}
