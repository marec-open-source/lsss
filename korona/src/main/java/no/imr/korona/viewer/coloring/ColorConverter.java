package no.imr.korona.viewer.coloring;

import no.imr.korona.color.Colormap;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.DiscreteVariable;
import org.jspecify.annotations.Nullable;

import java.awt.Graphics2D;
import java.awt.Point;

/**
 * Converts echogram values and categorisation to color.
 */
public abstract sealed class ColorConverter implements PingToColor
      permits DiscreteColorConverter, SingleValueColorConverter {

   ColorConverter() {
   }

   public abstract ColorConverterType getType();

   public abstract void drawLegend(Graphics2D g, int width, int height, boolean drawInteractiveControls);

   public @Nullable String getToolTipText(Point point) {
      return null;
   }

   public @Nullable ContinuousVariable getContinuousVariable() {
      return null;
   }

   public @Nullable DiscreteVariable getDiscreteVariable() {
      return null;
   }

   public @Nullable Colormap getColormap() {
      return null;
   }

   public boolean isUsableInContext() {
      DiscreteVariable discreteVariable = getDiscreteVariable();
      ContinuousVariable continuousVariable = getContinuousVariable();
      return (discreteVariable == null || discreteVariable.isUsableInContext()) &&
            (continuousVariable == null || continuousVariable.isUsableInContext());
   }

   /**
    * Set Sv range, and calculate color.
    */
   public void update() {
   }
}
