package no.imr.tools.swing;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * Positions a component using the component's preferred size at most.
 */
public final class PreferredSizeLayout implements LayoutManager {
   private final HorizontalAlignment horizontalAlignment;

   private PreferredSizeLayout(HorizontalAlignment horizontalAlignment) {
      this.horizontalAlignment = horizontalAlignment;
   }

   public static JPanel wrap(Component component, HorizontalAlignment horizontalAlignment) {
      JPanel panel = new JPanel(new PreferredSizeLayout(horizontalAlignment));
      panel.add(component);
      return panel;
   }

   @Override
   public void addLayoutComponent(String name, Component comp) {
   }

   @Override
   public void removeLayoutComponent(Component comp) {
   }

   @Override
   public Dimension preferredLayoutSize(Container parent) {
      return parent.getComponentCount() == 0
            ? new Dimension(0, 0)
            : parent.getComponent(0).getPreferredSize();
   }

   @Override
   public Dimension minimumLayoutSize(Container parent) {
      return parent.getComponentCount() == 0
            ? new Dimension(0, 0)
            : parent.getComponent(0).getMinimumSize();
   }

   @Override
   public void layoutContainer(Container parent) {
      if (parent.getComponentCount() == 0) {
         return;
      }
      Component component = parent.getComponent(0);
      int parentWidth = parent.getWidth();
      int componentWidth = Math.min(component.getPreferredSize().width, parentWidth);
      int leftoverWidth = Math.max(0, parentWidth - componentWidth);
      int x = Math.round(leftoverWidth * horizontalAlignment.leftFraction);
      component.setBounds(x, 0, componentWidth, parent.getHeight());
   }

   public enum HorizontalAlignment {
      LEFT(0), CENTER(0.5f), RIGHT(1);

      private final float leftFraction;

      HorizontalAlignment(float leftFraction) {
         this.leftFraction = leftFraction;
      }
   }
}
