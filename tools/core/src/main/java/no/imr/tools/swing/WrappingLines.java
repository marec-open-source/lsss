package no.imr.tools.swing;

import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Places components in lines given a max width and explicit line breaks.
 */
public final class WrappingLines {
   private final int horizontalGap;
   private final int verticalGap;
   private final int lineBreakGap;
   private final JPanel panel = new JPanel(new GridBagLayout());
   private final List<@Nullable Component> components = new ArrayList<>();

   public WrappingLines(int horizontalGap, int verticalGap, int lineBreakGap) {
      this.horizontalGap = horizontalGap;
      this.verticalGap = verticalGap;
      this.lineBreakGap = lineBreakGap;
   }

   public void clear() {
      components.clear();
   }

   public void add(Component component) {
      components.add(component);
   }

   public void addLineBreak() {
      components.add(null);
   }

   public JComponent getPanel() {
      return panel;
   }

   public Dimension relayout(int maxWidth) {
      panel.removeAll();

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 1;
      constraints.anchor = GridBagConstraints.WEST;

      int totalWidth = 0;
      int totalHeight = 0;
      int currentRowWidth = 0;
      int currentRowHeight = 0;
      Box currentRow = Box.createHorizontalBox();
      boolean didLineBreak = false;
      for (int i = 0; true; i++) {
         boolean end = i == components.size();
         Component component = end ? null : components.get(i);
         Dimension size = component != null ? component.getPreferredSize() : null;
         if (size == null || currentRow.getComponentCount() > 0 && currentRowWidth + horizontalGap + size.width > maxWidth) {
            // New line
            if (currentRow.getComponentCount() > 0) {
               // Row is not empty, so it must be added
               if (panel.getComponentCount() > 0) {
                  // Has previous lines, so must first add vertical separator
                  int verticalSpace = didLineBreak ? lineBreakGap : verticalGap;
                  totalHeight += verticalSpace;
                  panel.add(Box.createVerticalStrut(verticalSpace), constraints);
               }
               totalHeight += currentRowHeight;
               totalWidth = Math.max(totalWidth, currentRowWidth);
               panel.add(currentRow, constraints);

               // Prepare for new row
               currentRow = Box.createHorizontalBox();
               currentRowWidth = 0;
               currentRowHeight = 0;
               didLineBreak = size == null;
            }
            if (end) {
               break;
            }
            if (size == null) {
               continue;
            }
         }
         if (currentRow.getComponentCount() > 0) {
            currentRowWidth += horizontalGap;
            currentRow.add(Box.createHorizontalStrut(horizontalGap));
         }
         currentRowWidth += size.width;
         currentRowHeight = Math.max(currentRowHeight, size.height);
         currentRow.add(component);
      }

      panel.validate();
      panel.repaint();

      return new Dimension(totalWidth, totalHeight);
   }
}
