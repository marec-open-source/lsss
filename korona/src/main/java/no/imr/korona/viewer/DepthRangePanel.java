package no.imr.korona.viewer;

import no.imr.tools.Utils;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays depth ranges.
 */
public final class DepthRangePanel {
   private static final int PREFERRED_WIDTH = 60;

   private final DepthRangeComponent component = new DepthRangeComponent();
   private final DepthRangeChooser depthRangeChooser;
   private FloatRange maxDepthRange = FloatRange.EMPTY_RANGE;
   private final List<FloatRange> depthRanges = new ArrayList<>();

   public DepthRangePanel(DepthRangeChooser depthRangeChooser) {
      this.depthRangeChooser = depthRangeChooser;

      component.setPreferredSize(new Dimension(PREFERRED_WIDTH, 600));

      MyListener listener = new MyListener();
      component.addMouseListener(listener);
      component.addMouseMotionListener(listener);
   }

   public JComponent getComponent() {
      return component;
   }

   private JPopupMenu makePopupMenu() {
      JPopupMenu popupMenu = new JPopupMenu();

      addDepthRangeModeItem(popupMenu, DepthRangeMode.AUTO, "Automatically choose best depth range");
      addDepthRangeModeItem(popupMenu, DepthRangeMode.MAX, "Automatically choose maximum depth range");
      addDepthRangeModeItem(popupMenu, DepthRangeMode.MANUAL, "Manually adjust depth range");

      popupMenu.addSeparator();

      JMenuItem showNumericChooser = popupMenu.add("Set depth range...");
      showNumericChooser.setEnabled(depthRangeChooser.getDepthRangeMode() == DepthRangeMode.MANUAL && !maxDepthRange.isEmpty());
      showNumericChooser.addActionListener(_ -> new MinMaxDialog(component, "Depth Range",
            depthRangeChooser.getViewDepthRange().expandToMultipleOf(1), 1, maxDepthRange, depthRangeChooser::setFixedDepthRange));

      return popupMenu;
   }

   private void addDepthRangeModeItem(JPopupMenu popupMenu, DepthRangeMode depthRangeMode, String tooltip) {
      boolean selected = depthRangeChooser.getDepthRangeMode() == depthRangeMode;
      JMenuItem item = MiscIcons.check(selected).on(popupMenu.add(depthRangeMode.toString()));
      item.setToolTipText(tooltip);
      item.addActionListener(_ -> depthRangeChooser.setDepthRangeMode(depthRangeMode));
   }

   private final class MyListener extends MouseAdapter {
      private boolean doAdjust;
      private boolean adjustMin;

      private MyListener() {
      }

      @Override
      public void mousePressed(MouseEvent e) {
         if (SwingUtilities.isLeftMouseButton(e) && !maxDepthRange.isEmpty()) {
            doAdjust = true;
            adjustMin = depthRangeChooser.getViewDepthRange().valueToFraction(yToDepth(e.getY())) < 0.5;
            update(e);
         }

         maybeShowPopup(e);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         if (doAdjust) {
            update(e);
         }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         doAdjust = false;
         maybeShowPopup(e);
      }

      private void maybeShowPopup(MouseEvent e) {
         if (e.isPopupTrigger()) {
            makePopupMenu().show(e.getComponent(), e.getX(), e.getY());
         }
      }

      private void update(MouseEvent mouseEvent) {
         float depth = yToDepth(mouseEvent.getY());
         setMinOrMax(depth, adjustMin);
      }
   }

   private void setMinOrMax(float depth, boolean setMin) {
      if (setMin) {
         depthRangeChooser.setMinFixedDepth(Math.clamp(depth, maxDepthRange.min(), maxDepthRange.max() - 1));
      } else {
         depthRangeChooser.setMaxFixedDepth(Math.clamp(depth, maxDepthRange.min() + 1, maxDepthRange.max()));
      }

      repaint();
   }

   private void repaint() {
      component.repaint();
   }

   private int getWidth() {
      return component.getWidth();
   }

   private int getHeight() {
      return component.getHeight();
   }

   private final class DepthRangeComponent extends JComponent {
      private DepthRangeComponent() {
      }

      @Override
      protected void paintComponent(Graphics g) {
         privatePaint((Graphics2D) g);
      }
   }

   private void privatePaint(Graphics2D g) {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, getWidth(), getHeight());

      maxDepthRange = depthRangeChooser.getMaxDepthRange().expandToMultipleOf(1);
      if (!maxDepthRange.isEmpty()) {
         drawLines(g);
         drawTexts(g);
         drawViewMarkers(g);
      }
   }

   private float yToDepth(float y) {
      return maxDepthRange.fractionToValue(y / (getHeight() - 1));
   }

   private int depthToY(float depth) {
      return Math.round((getHeight() - 1) * maxDepthRange.valueToFraction(depth));
   }

   private void drawTexts(Graphics2D g) {
      int textHeight = g.getFontMetrics().getHeight();
      int textCount = getHeight() / (textHeight * 3);
      if (textCount < 2) {
         return;
      }

      float delta = (float) NiceNumber.niceNumber(maxDepthRange.getSize() / textCount, true);

      FloatRange shrunkRange = maxDepthRange.shrinkToMultipleOf(delta);
      if (shrunkRange == FloatRange.EMPTY_RANGE) {
         return;
      }
      int count = Math.round(shrunkRange.getSize() / delta) + 1;

      String format = Utils.getPrecisionString(delta);

      for (int i = 0; i < count; i++) {
         float depth = shrunkRange.min() + i * delta;
         int y = Math.round(getHeight() * maxDepthRange.valueToFraction(depth));
         String text = Utils.format(format, depth);
         GuiText.draw(g, text, Color.BLACK, getWidth() - 20, y, GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.CENTER, null);
      }
   }

   private void drawViewMarkers(Graphics2D g) {
      FloatRange viewRange = depthRangeChooser.getViewDepthRange();

      g.setColor(Color.BLACK);

      drawHorizontalLine(g, depthToY(viewRange.min()));
      drawHorizontalLine(g, depthToY(viewRange.max()));
   }

   private void drawHorizontalLine(Graphics2D g, int y) {
      g.drawLine(0, y, getWidth(), y);
   }

   /**
    * Draw all lines describing depth for all channels.
    *
    * @param g graphic to draw
    */
   private void drawLines(Graphics2D g) {
      int channelCount = depthRangeChooser.getDepthRangeLists().size();
      if (channelCount == 0) {
         return;
      }
      int dx = getWidth() / channelCount;
      int x = dx - 1;
      for (int i = 0; i < channelCount; i++, x += dx) {
         depthRangeChooser.getDepthRangeLists().get(i).getAllDepthRanges(depthRanges, dx);
         drawLineChannel(g, x, depthRanges);
      }
   }

   private void drawLineChannel(Graphics2D g, int xMax, List<FloatRange> depthRanges) {
      g.setColor(Color.GRAY);
      for (int i = 0; i < depthRanges.size(); i++) {
         FloatRange depthRange = depthRanges.get(i);
         int x = xMax - i;
         int y1 = depthToY(depthRange.min());
         int y2 = depthToY(depthRange.max());
         g.drawLine(x, y1, x, y2);

         if (i == 0) {
            g.setColor(Color.LIGHT_GRAY);
         }
      }
   }
}
