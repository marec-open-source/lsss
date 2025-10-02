package no.imr.korona.viewer;

import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.coloring.DiscreteColorConverter;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.DiscreteVariableSettings;
import no.imr.tools.swing.overlay.Overlay;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

final class SvColorPanelColorBarOverlay extends Overlay {
   private final ColorConverterContainer converterContainer;
   private @Nullable Drag drag;

   SvColorPanelColorBarOverlay(ColorConverterContainer converterContainer) {
      this.converterContainer = converterContainer;
   }

   @Override
   public void draw(Graphics2D g2d) {
      converterContainer.getColorConverter().drawLegend(g2d, getWidth(), getHeight(), true);
   }

   @Override
   public boolean overlaps(Rectangle2D rectangle2D) {
      return true;
   }

   @Override
   public void mouseClicked(MouseEvent e) {
      int categoryIndex = getCategoryIndex(e);
      if (categoryIndex != -1) {
         DiscreteColorConverter discreteColorConverter = (DiscreteColorConverter) converterContainer.getColorConverter();
         DiscreteVariableSettings settings = discreteColorConverter.getDiscreteVariable().getSettings();
         DiscreteCategory category = settings.getCategories().get(categoryIndex);
         settings.setPlottable(category, !settings.isPlottable(category));
      }
   }

   private int getCategoryIndex(MouseEvent e) {
      if (converterContainer.getColorConverter() instanceof DiscreteColorConverter discreteColorConverter) {
         return discreteColorConverter.getCategoryIndex(getWidth(), getHeight(), e.getX(), e.getY());
      }
      return -1;
   }

   @Override
   public void mousePressed(MouseEvent e) {
      if (getCategoryIndex(e) != -1) {
         drag = null;
         return;
      }
      ContinuousVariable variable = converterContainer.getColorConverter().getContinuousVariable();
      if (SwingUtilities.isLeftMouseButton(e) && variable != null) {
         boolean setMin = isClosestToMin(variable, e.getY());
         drag = new Drag(variable, setMin);
         drag.setMinOrMax(e.getY());
      }
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      drag = null;
   }

   private boolean isClosestToMin(ContinuousVariable variable, int y) {
      float value = getValue(variable, y);
      return variable.getSettings().getRange().valueToFraction(value) < 0.5;
   }

   private float getValue(ContinuousVariable variable, int y) {
      float fraction = 1 - (float) y / (float) (getHeight() - 1);
      return variable.getSettings().getMaxRange().fractionToValue(fraction);
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      if (drag != null) {
         drag.setMinOrMax(e.getY());
      }
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      getOverlaidComponent().setToolTipText(converterContainer.getColorConverter().getToolTipText(e.getPoint()));
   }

   private final class Drag {
      private final ContinuousVariable variable;
      private final boolean setMin;

      private Drag(ContinuousVariable variable, boolean setMin) {
         this.variable = variable;
         this.setMin = setMin;
      }

      private void setMinOrMax(int y) {
         if (setMin) {
            variable.getSettings().setMin(getValue(variable, y));
         } else {
            variable.getSettings().setMax(getValue(variable, y));
         }
      }
   }
}
