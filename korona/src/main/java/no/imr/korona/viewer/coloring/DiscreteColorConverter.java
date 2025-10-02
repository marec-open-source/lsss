package no.imr.korona.viewer.coloring;

import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.tools.swing.GuiText;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Base class for color conversion of categorization and plankton inversion.
 */
public abstract non-sealed class DiscreteColorConverter extends ColorConverter {
   private final DiscreteVariable discreteVariable;

   DiscreteColorConverter(DiscreteVariable discreteVariable) {
      this.discreteVariable = discreteVariable;
   }

   @Override
   public DiscreteVariable getDiscreteVariable() {
      return discreteVariable;
   }

   void drawAddedLegend(Graphics2D g, int width, int height) {
   }

   @Override
   public void drawLegend(Graphics2D g, int width, int height, boolean drawInteractiveControls) {
      // Clear
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, width, height);

      // Fill from subclasses
      drawAddedLegend(g, width, height);

      // Draw simple category
      drawCategoryLegend(g, width, height);
   }

   private void drawCategoryLegend(Graphics2D g, int width, int height) {
      int noCol = discreteVariable.getSettings().getCategories().size();
      int deltaY = height / (noCol + 1);
      int sizeRect = width / 3;
      int startRectX = 2;

      for (int i = 0, y = deltaY; i < noCol; i++, y += deltaY) {
         DiscreteCategory category = discreteVariable.getSettings().getCategories().get(i);

         g.setColor(Color.BLACK);
         GuiText.draw(g, category.getLegend(), Color.BLACK, 2, y - 1, GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.BOTTOM, null);

         g.setColor(category.getColor());
         if (discreteVariable.getSettings().isPlottable(category)) {
            g.fillRect(startRectX, y, sizeRect, sizeRect);
         } else {
            g.drawRect(startRectX, y, sizeRect, sizeRect);
         }
      }
   }

   public int getCategoryIndex(int width, int height, int x, int y) {
      int noCol = discreteVariable.getSettings().getCategories().size();
      int deltaY = height / (noCol + 1);
      int sizeRect = width / 3;
      int startRectX = 2;

      if (x >= startRectX && x < startRectX + sizeRect
            && y >= deltaY && y % deltaY < sizeRect) {
         return y / deltaY - 1;
      } else {
         return -1;
      }
   }
}
