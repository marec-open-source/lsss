package no.imr.tools.swing;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.function.Function;

final class ContrastingColorDialog {
   ContrastingColorDialog(String title, Function<Color, Color> otherColor) {
      Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

      int n = bounds.height / 15;
      JPanel colorPanel = new JPanel(new GridLayout(n, n));

      JLabel[][] labels = new JLabel[n][n];
      for (int row = n - 1; row >= 0; row--) {
         for (int column = 0; column < n; column++) {
            JLabel label = new ColorJLabel(Integer.toString(Math.min(row, column) + 1));
            ToolTipManager.sharedInstance().registerComponent(label);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setOpaque(true);
            labels[row][column] = label;
            colorPanel.add(label);
         }
      }

      int hueMax = 1000;
      JSlider hueSlider = new JSlider(0, hueMax - 1, hueMax / 2);

      JToggleButton flipButton = new JToggleButton("Flip");

      Runnable update = () -> {
         float hue = (hueSlider.getValue() + 0.5f) / hueMax;
         colorPanel.repaint();
         for (int row = 0; row < n; row++) {
            float brightness = (row + 0.5f) / n;
            for (int column = 0; column < n; column++) {
               float saturation = (column + 0.5f) / n;
               Color color = Color.getHSBColor(hue, saturation, brightness);
               JLabel label = labels[row][column];
               if (flipButton.isSelected()) {
                  label.setForeground(color);
                  label.setBackground(otherColor.apply(color));
               } else {
                  label.setForeground(otherColor.apply(color));
                  label.setBackground(color);
               }
            }
         }
      };

      update.run();
      hueSlider.addChangeListener(e -> update.run());
      flipButton.addChangeListener(e -> update.run());

      JPanel sliderPanel = new JPanel(new BorderLayout());
      sliderPanel.add(hueSlider);
      sliderPanel.add(flipButton, BorderLayout.EAST);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(colorPanel);
      mainPanel.add(sliderPanel, BorderLayout.SOUTH);

      JFrame frame = new JFrame(title);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(mainPanel);
      frame.setBounds(bounds);
      frame.setVisible(true);
   }

   private static final class ColorJLabel extends JLabel {
      private ColorJLabel(String text) {
         super(text);
      }

      @Override
      public String getToolTipText() {
         Color fg = getForeground();
         float[] fgHSB = Color.RGBtoHSB(fg.getRed(), fg.getGreen(), fg.getBlue(), null);
         Color bg = getBackground();
         float[] bgHSB = Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null);
         return "<html><table>"
               + "<tr><th></th><th>Foreground</th><th>Background</th></tr>"
               + "<tr><td>H</td><td>" + fgHSB[0] + "</td><td>" + bgHSB[0] + "</td></tr>"
               + "<tr><td>S</td><td>" + fgHSB[1] + "</td><td>" + bgHSB[1] + "</td></tr>"
               + "<tr><td>B</td><td>" + fgHSB[2] + "</td><td>" + bgHSB[2] + "</td></tr>"
               + "<tr><td>Rel. lum.</td><td>" + (float) ColorUtils.relativeLuminance(fg)
               /**/ + "</td><td>" + (float) ColorUtils.relativeLuminance(bg) + "</td></tr>"
               + "<tr><td>Contrast</td><td colspan=2>" + (float) ColorUtils.contrastRatio(fg, bg) + "</td></tr>"
               + "</table>";
      }
   }
}
