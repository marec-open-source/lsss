package no.imr.tools.swing;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * A container for two components that may be visible or not.
 * If both are visible a JSplitPane is used, otherwise a JPanel.
 */
public final class SplitPaneContainer {
   private final JPanel panel = new JPanel(new BorderLayout());
   private final JSplitPane splitPane;
   private final JComponent leftComponent;
   private final JComponent rightComponent;
   private int dividerLocation;

   public SplitPaneContainer(int orientation, JComponent leftComponent, JComponent rightComponent) {
      splitPane = new JSplitPane(orientation);
      splitPane.setBorder(BorderFactory.createEmptyBorder());
      this.leftComponent = leftComponent;
      this.rightComponent = rightComponent;
      setBothVisible();
   }

   public JComponent getComponent() {
      return panel;
   }

   public JSplitPane getSplitPane() {
      return splitPane;
   }

   public boolean isHorizontal() {
      return splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
   }

   public void setLeftVisible(boolean leftVisible) {
      setVisible(leftComponent, leftVisible);
   }

   public void setRightVisible(boolean rightVisible) {
      setVisible(rightComponent, rightVisible);
   }

   public void setDividerLocation(int dividerLocation) {
      this.dividerLocation = dividerLocation;
      splitPane.setDividerLocation(dividerLocation);
   }

   public void setVisible(JComponent component, boolean visible) {
      boolean componentVisible = isVisible(component);
      if (componentVisible == visible) {
         return;
      }

      JComponent otherComponent = component == leftComponent ? rightComponent : leftComponent;
      boolean otherComponentVisible = isVisible(otherComponent);

      if (componentVisible && otherComponentVisible) {
         dividerLocation = splitPane.getDividerLocation();
      }

      panel.removeAll();
      splitPane.setLeftComponent(null);
      splitPane.setRightComponent(null);

      if (visible && otherComponentVisible) {
         setBothVisible();
         panel.validate();
         Dimension leftMinimumSize = leftComponent.getMinimumSize();
         if (leftMinimumSize != null) {
            dividerLocation = Math.max(dividerLocation, getSize(leftMinimumSize));
         }
         Dimension rightMinimumSize = rightComponent.getMinimumSize();
         if (rightMinimumSize != null) {
            dividerLocation = Math.min(dividerLocation, getSize(splitPane) - UiUtils.splitPaneDividerSize() - getSize(rightMinimumSize));
         }
         splitPane.setDividerLocation(dividerLocation);
      } else if (visible) {
         panel.add(component);
      } else if (otherComponentVisible) {
         panel.add(otherComponent);
      } else {
         // do nothing
      }

      panel.validate();
   }

   private void setBothVisible() {
      panel.add(splitPane);
      splitPane.setLeftComponent(leftComponent);
      splitPane.setRightComponent(rightComponent);
   }

   private static boolean isVisible(JComponent component) {
      return component.getParent() != null;
   }

   public int getSize() {
      return getSize(panel);
   }

   public int getSize(Dimension dimension) {
      return isHorizontal() ? dimension.width : dimension.height;
   }

   public int getSize(Component component) {
      return isHorizontal() ? component.getWidth() : component.getHeight();
   }
}
