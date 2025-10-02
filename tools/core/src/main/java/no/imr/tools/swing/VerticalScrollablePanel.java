package no.imr.tools.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.LayoutManager;

/**
 * A JPanel that implements the Scrollable interface and tracks the viewport width.
 */
public final class VerticalScrollablePanel extends ScrollablePanel {
   public VerticalScrollablePanel(LayoutManager layout) {
      super(layout);

      setScrollableTracksViewportWidth(true);
   }

   public static VerticalScrollablePanel wrap(Component component) {
      VerticalScrollablePanel panel = new VerticalScrollablePanel(new BorderLayout());
      panel.add(component);
      return panel;
   }
}
