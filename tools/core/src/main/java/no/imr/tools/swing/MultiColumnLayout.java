package no.imr.tools.swing;

import javax.swing.JViewport;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public final class MultiColumnLayout implements LayoutManager {
   private int preferredMaxHeight;
   private boolean horizontalFill;

   public MultiColumnLayout() {
   }

   public static void addRelayoutListener(Container sizeContainer, Container layoutTarget) {
      sizeContainer.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            layoutTarget.invalidate();
         }
      });
   }

   public void setPreferredMaxHeight(int preferredMaxHeight) {
      this.preferredMaxHeight = preferredMaxHeight;
   }

   public void setHorizontalFill(boolean horizontalFill) {
      this.horizontalFill = horizontalFill;
   }

   @Override
   public void addLayoutComponent(String name, Component comp) {
   }

   @Override
   public void removeLayoutComponent(Component comp) {
   }

   @Override
   public Dimension preferredLayoutSize(Container parent) {
      return doLayout(parent, 0, false);
   }

   @Override
   public Dimension minimumLayoutSize(Container parent) {
      return doLayout(parent, 0, false);
   }

   @Override
   public void layoutContainer(Container parent) {
      doLayout(parent, 0, true);
   }

   private Dimension doLayout(Container parent, int maxBelowCount, boolean moveComponents) {
      Container grandParent = parent.getParent();
      Dimension size = grandParent instanceof JViewport ? grandParent.getSize() : parent.getSize();
      Insets insets = parent.getInsets();

      int availableWidth = size.width;
      int availableHeight = preferredMaxHeight > 0 ? preferredMaxHeight : size.height;
      int xLimit = availableWidth - insets.right;
      int yLimit = availableHeight - insets.bottom;

      int x = insets.left;
      int y = insets.top;

      int maxX = x;
      int maxY = y;

      int column = 0;
      int columnWidth = 0;
      int columnStartIndex = 0;
      int belowCount = 0;

      int componentCount = parent.getComponentCount();
      for (int i = 0; i < componentCount; i++) {
         Component c = parent.getComponent(i);
         if (!c.isVisible()) {
            continue;
         }
         Dimension d = c.getPreferredSize();
         if (!c.isPreferredSizeSet()) {
            // Avoid that preferred size changes later if actual size changes.
            c.setPreferredSize(d);
         }
         if (y + d.height > yLimit) {
            if (belowCount < maxBelowCount) {
               belowCount++;
            } else {
               column++;
               columnWidth = d.width;
               columnStartIndex = i;
               belowCount = 0;
               x = maxX;
               y = insets.top;
            }
         }
         if (x + d.width > xLimit && column > 0 && preferredMaxHeight == 0) {
            return doLayout(parent, maxBelowCount + 1, moveComponents);
         }
         int boundsWidth;
         if (horizontalFill) {
            if (columnWidth < d.width) {
               columnWidth = d.width;
               belowCount = 0;
               y = insets.top;
               i = columnStartIndex - 1;
               continue;
            }
            boundsWidth = columnWidth;
         } else {
            boundsWidth = d.width;
         }
         if (moveComponents) {
            c.setBounds(x, y, boundsWidth, d.height);
         }
         y += d.height;
         maxX = Math.max(maxX, x + d.width);
         maxY = Math.max(maxY, y);
      }

      return new Dimension(maxX + insets.right, maxY + insets.bottom);
   }
}
