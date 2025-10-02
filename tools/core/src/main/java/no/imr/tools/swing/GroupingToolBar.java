package no.imr.tools.swing;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * A toolbar with groups of components.
 */
public final class GroupingToolBar {
   private static final ToolBarGroup SEPARATOR = new ToolBarGroup(List.of());
   private static final Insets BUTTON_MARGIN = new Insets(2, 2, 2, 2);
   private static final Dimension MAXIMUM_SIZE = new Dimension(1000, 25);
   private static final Dimension SEPARATOR_MIN_SIZE = new Dimension(18, 1);

   private final JToolBar toolBar = new JToolBar();
   private final List<ToolBarGroup> groups = new ArrayList<>();

   public GroupingToolBar() {
      toolBar.setFloatable(false);
      toolBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, GuiUtils.SEPARATOR_COLOR),
            BorderFactory.createEmptyBorder(1, 1, 0, 1)));
   }

   public JToolBar getToolBar() {
      return toolBar;
   }

   public void add(JComponent component) {
      add(List.of(component));
   }

   public void add(List<? extends JComponent> components) {
      addGroup(new ToolBarGroup(components));
   }

   public void addSeparator() {
      addGroup(SEPARATOR);
   }

   private void addGroup(ToolBarGroup group) {
      groups.add(group);
   }

   public void update() {
      SwingDelayer.invokeLater(toolBar, () -> {
         toolBar.removeAll();
         boolean needSeparator = false;
         for (ToolBarGroup group : groups) {
            if (group.equals(SEPARATOR)) {
               needSeparator = true;
               continue;
            }
            if (group.components.isEmpty()) {
               continue;
            }
            if (needSeparator) {
               toolBar.add(new ToolBarSeparator());
               needSeparator = false;
            }
            for (JComponent component : group.components) {
               if (component instanceof AbstractButton button) {
                  button.setMargin(BUTTON_MARGIN);
               }
               component.setMaximumSize(MAXIMUM_SIZE);
               component.setAlignmentY(JComponent.TOP_ALIGNMENT);
               toolBar.add(component);
            }
         }
         toolBar.add(GuiUtils.createHorizontalFiller());
         toolBar.revalidate();
         toolBar.repaint();
      });
   }

   private record ToolBarGroup(List<? extends JComponent> components) {
   }

   public static final class ToolBarSeparator extends JComponent {
      public ToolBarSeparator() {
         setFocusable(false);
         setMinimumSize(SEPARATOR_MIN_SIZE);
         setAlignmentY(TOP_ALIGNMENT);
      }

      @Override
      protected void paintComponent(Graphics g) {
         g.setColor(GuiUtils.SEPARATOR_COLOR);
         int x = getWidth() / 2;
         g.drawLine(x, 2, x, getHeight() - 4);
      }
   }
}
