package no.imr.tools.visualizer;

import no.imr.tools.plot.PlotChartPanel;
import no.imr.tools.plot.PlotCoordinateConverter;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.swing.FocusableComponentMouseListener;
import org.jfree.chart.JFreeChart;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

final class ItemScatterChartPanel<T> extends PlotChartPanel {
   private final ItemScatter<T> itemScatter;
   private @Nullable Rectangle2D selectionRectangle;
   private @Nullable Point2D referencePoint;

   ItemScatterChartPanel(ItemScatter<T> itemScatter, JFreeChart chart) {
      super(chart);

      this.itemScatter = itemScatter;
      setDismissDelay(Integer.MAX_VALUE);

      addMouseListener(new FocusableComponentMouseListener(this));
      addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_ESCAPE -> {
                  referencePoint = null;
                  selectionRectangle = null;
                  repaint();
               }
               case KeyEvent.VK_HOME -> {
                  itemScatter.update();
               }
               default -> {
               }
            }
         }
      });
   }

   @Override
   public @Nullable String getToolTipText(MouseEvent e) {
      T item = getClosestItem(e.getPoint());
      return item != null ? ItemUtils.getToolTipText(item, itemScatter.getAllFeatures()) : null;
   }

   private @Nullable T getClosestItem(Point p) {
      PlotCoordinateConverter converter = new PlotCoordinateConverter(this);
      T closestItem = null;
      double minDistSq = 25;
      for (T item : itemScatter.getItemContainer().getAllItems()) {
         double dx = p.x - converter.xDataToScreen(itemScatter.getXFeature().itemToDouble.applyAsDouble(item));
         double dy = p.y - converter.yDataToScreen(itemScatter.getYFeature().itemToDouble.applyAsDouble(item));
         double distSq = dx * dx + dy * dy;
         if (distSq < minDistSq) {
            minDistSq = distSq;
            closestItem = item;
         }
      }
      return closestItem;
   }

   @Override
   public void mousePressed(MouseEvent e) {
      if (!SwingUtilities.isLeftMouseButton(e)) {
         super.mousePressed(e);
         return;
      }
      referencePoint = e.getPoint();
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      if (referencePoint == null) {
         super.mouseDragged(e);
         return;
      }
      Graphics2D g2d = (Graphics2D) getGraphics();
      g2d.setXORMode(Color.GRAY);
      if (selectionRectangle != null) {
         g2d.draw(selectionRectangle);
      } else {
         selectionRectangle = new Rectangle2D.Double();
      }
      Rectangle2D screenDataArea = getScreenDataArea();
      double x = Math.clamp(e.getX(), screenDataArea.getMinX(), screenDataArea.getMaxX());
      double y = Math.clamp(e.getY(), screenDataArea.getMinY(), screenDataArea.getMaxY());
      selectionRectangle.setFrameFromDiagonal(referencePoint.getX(), referencePoint.getY(), x, y);

      g2d.draw(selectionRectangle);
      g2d.dispose();
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      if (referencePoint == null) {
         super.mouseReleased(e);
         return;
      }
      if (selectionRectangle != null) {
         Rectangle2D selection = PlotUtils.screenToData(this, selectionRectangle);
         itemScatter.select(selection, e);
         selectionRectangle = null;
      }
      referencePoint = null;
      repaint();
   }
}
