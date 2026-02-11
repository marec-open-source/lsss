package no.imr.tools.swing;

import no.imr.tools.concurrent.Exec;
import no.imr.tools.listening.ChangeManager;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A split pane with any number of components.
 */
public final class MultiSplitPane {
   private int orientation;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final List<JSplitPane> splitPanes = new ArrayList<>();
   private final List<JComponent> components = new ArrayList<>();
   private final ChangeManager changeManager = new ChangeManager();
   private @Nullable Future<?> distributeEvenlyFuture;

   public MultiSplitPane(int orientation) {
      this.orientation = orientation;
   }

   public void add(JComponent component) {
      components.add(component);
      update();
   }

   public void addAll(Collection<? extends JComponent> moreComponents) {
      components.addAll(moreComponents);
      update();
   }

   public JComponent getPanel() {
      return mainPanel;
   }

   public List<JComponent> getComponents() {
      return components;
   }

   public void setSplitterPositions(List<Integer> positions) {
      int max = Math.min(positions.size(), splitPanes.size());
      for (int i = 0; i < max; i++) {
         splitPanes.get(i).setDividerLocation(positions.get(i));
      }
   }

   private void update() {
      mainPanel.removeAll();
      splitPanes.clear();

      if (!components.isEmpty()) {
         Component lastComponent = components.getLast();
         for (int i = components.size() - 2; i >= 0; i--) {
            Component component = components.get(i);
            JSplitPane splitPane = createSplitPane(component, lastComponent);
            splitPanes.add(splitPane);
            lastComponent = splitPane;
         }
         Collections.reverse(splitPanes);
         mainPanel.add(lastComponent);
      }

      distributeEvenly();
   }

   public void distributeEvenly() {
      if (distributeEvenlyFuture != null) {
         distributeEvenlyFuture.cancel(true);
         distributeEvenlyFuture = null;
      }
      if (getSize(mainPanel) == 0) {
         distributeEvenlyFuture = Exec.schedule(() -> SwingUtilities.invokeLater(this::distributeEvenly), 1, TimeUnit.MILLISECONDS);
         return;
      }

      double sizePerComponent = components.isEmpty() ? 0 : (double) getAvailableSize() / components.size();
      int sizePerComponentInt = (int) sizePerComponent;
      double sizePerComponentFraction = sizePerComponent - sizePerComponentInt;
      double accumulatedFraction = 0;
      for (int i = 0; i < splitPanes.size(); i++) {
         JSplitPane splitPane = splitPanes.get(i);
         int size = sizePerComponentInt;
         accumulatedFraction += sizePerComponentFraction;
         if (accumulatedFraction >= 1) {
            size += 1;
            accumulatedFraction -= 1;
         }
         splitPane.setDividerLocation(size);
         splitPane.setResizeWeight(1.0 / (components.size() - i));
         splitPane.validate();
      }

      mainPanel.validate();
      mainPanel.repaint();
   }

   public void clear() {
      components.clear();
      update();
   }

   public boolean isHorizontal() {
      return orientation == JSplitPane.HORIZONTAL_SPLIT;
   }

   public void changeSplitDirection() {
      orientation = isHorizontal() ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
      update();
   }

   private JSplitPane createSplitPane(Component leftComponent, Component rightComponent) {
      JSplitPane splitPane = new JSplitPane(orientation, leftComponent, rightComponent);

      new SplitPositionListener(splitPane, _ -> changeManager.notifyListeners());

      splitPane.setBorder(BorderFactory.createEmptyBorder());
      return splitPane;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public List<JSplitPane> getSplitPanes() {
      return splitPanes;
   }

   public int getSize(Dimension dimension) {
      return isHorizontal() ? dimension.width : dimension.height;
   }

   public int getSize(Component component) {
      return isHorizontal() ? component.getWidth() : component.getHeight();
   }

   public int getAvailableSize() {
      int availableSize = getSize(mainPanel);
      if (!splitPanes.isEmpty()) {
         availableSize -= splitPanes.size() * UiUtils.splitPaneDividerSize();
      }
      return availableSize;
   }
}
