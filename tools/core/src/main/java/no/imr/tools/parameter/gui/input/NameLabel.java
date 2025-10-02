package no.imr.tools.parameter.gui.input;

import no.imr.tools.swing.UiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

final class NameLabel extends JLabel {
   private @Nullable NameLabel parentNameLabel;
   private @Nullable Color highlightColor;

   NameLabel(String text) {
      super(text);
   }

   @Override
   protected void paintComponent(Graphics g) {
      if (highlightColor != null) {
         Color previousColor = g.getColor();
         g.setColor(highlightColor);
         g.fillRect(0, 0, getWidth(), getHeight());
         g.setColor(previousColor);
      }
      super.paintComponent(g);
   }

   void setParentNameLabel(NameLabel parentNameLabel) {
      this.parentNameLabel = parentNameLabel;
   }

   void setHighlight(@Nullable Color color) {
      highlightColor = color;
      repaint();
      if (parentNameLabel != null) {
         parentNameLabel.setHighlight(color);
      }
   }

   void addFocusListenerTo(JComponent component) {
      component.addFocusListener(new FocusListener() {
         @Override
         public void focusGained(FocusEvent e) {
            setHighlight(UiUtils.textFieldSelectionBackground());
         }

         @Override
         public void focusLost(FocusEvent e) {
            setHighlight(null);
         }
      });
   }
}
