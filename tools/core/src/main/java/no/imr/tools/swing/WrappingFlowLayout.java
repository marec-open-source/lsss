package no.imr.tools.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * A FlowLayout calculating its preferred size based on the parent's width.
 * Specially designed for use on panels which are added to scroll panes.
 */
public final class WrappingFlowLayout extends FlowLayout {
   public WrappingFlowLayout() {
   }

   public WrappingFlowLayout(int align) {
      super(align);
   }

   public WrappingFlowLayout(int align, int hgap, int vgap) {
      super(align, hgap, vgap);
   }

   @Override
   public Dimension preferredLayoutSize(Container target) {
      Container parent = target.getParent();
      if (parent == null) {
         return super.preferredLayoutSize(target);
      }

      synchronized (target.getTreeLock()) {
         Insets insets = target.getInsets();
         int borderWidth = insets.left + insets.right + getHgap() * 2;
         int borderHeight = insets.top + insets.bottom + getVgap() * 2;

         int maxWidth = parent.getWidth() - borderWidth;

         int componentCount = target.getComponentCount();
         boolean useBaseline = getAlignOnBaseline();

         Dimension totalDim = new Dimension(0, 0);
         for (int componentIndex = 0; componentIndex < componentCount; ) {
            boolean firstRowComponent = true;
            int rowWidth = 0;
            int rowHeight = 0;
            int maxAscent = 0;
            int maxDescent = 0;

            for (; componentIndex < componentCount; componentIndex++) {
               Component component = target.getComponent(componentIndex);
               if (component.isVisible()) {
                  Dimension d = component.getPreferredSize();
                  if (firstRowComponent) {
                     firstRowComponent = false;
                  } else {
                     if (rowWidth + d.width > maxWidth) {
                        break;
                     }
                     rowWidth += getHgap();
                  }
                  rowWidth += d.width;
                  rowHeight = Math.max(rowHeight, d.height);
                  if (useBaseline) {
                     int baseline = component.getBaseline(d.width, d.height);
                     if (baseline >= 0) {
                        maxAscent = Math.max(maxAscent, baseline);
                        maxDescent = Math.max(maxDescent, d.height - baseline);
                     }
                  }
               }
            }
            if (useBaseline) {
               rowHeight = Math.max(maxAscent + maxDescent, rowHeight);
            }

            if (firstRowComponent) {
               // No more visible components
               break;
            }

            totalDim.width = Math.max(totalDim.width, rowWidth);
            if (totalDim.height > 0) {
               totalDim.height += getVgap();
            }
            totalDim.height += rowHeight;
         }

         totalDim.width += borderWidth;
         totalDim.height += borderHeight;
         return totalDim;
      }
   }

   @Override
   public Dimension minimumLayoutSize(Container target) {
      return preferredLayoutSize(target);
   }
}
