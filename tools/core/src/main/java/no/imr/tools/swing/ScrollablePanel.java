package no.imr.tools.swing;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

public class ScrollablePanel extends JPanel implements Scrollable {
   private boolean scrollableTracksViewportWidth;

   public ScrollablePanel(LayoutManager layout) {
      super(layout);
   }

   @Override
   public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
   }

   @Override
   public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 10;
   }

   @Override
   public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
   }

   @Override
   public boolean getScrollableTracksViewportWidth() {
      return scrollableTracksViewportWidth;
   }

   public void setScrollableTracksViewportWidth(boolean scrollableTracksViewportWidth) {
      this.scrollableTracksViewportWidth = scrollableTracksViewportWidth;
   }

   @Override
   public boolean getScrollableTracksViewportHeight() {
      return false;
   }
}
