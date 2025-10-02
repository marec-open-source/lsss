package no.imr.tools.swing;

import no.imr.tools.Utils;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Displays the colors in {@link ColorUtils}.
 */
final class ColorNamesMain {
   private ColorNamesMain() {
   }

   private static void display() {
      List<NamedColor> namedColors = ColorUtils.getNameToColor().entrySet().stream()
            .map(entry -> new NamedColor(entry.getKey(), entry.getValue()))
            .collect(Collectors.toCollection(ArrayList::new));

      JPanel colorPanel = new VerticalScrollablePanel(new WrappingFlowLayout());
      Runnable resetColorPanel = () -> {
         colorPanel.removeAll();
         for (NamedColor namedColor : namedColors) {
            String name = namedColor.name;
            Color color = namedColor.color;
            JButton button = new JButton(name);
            button.setBackground(color);
            button.setForeground(ColorUtils.contrastingBlackOrWhite(color));
            float[] hsb = toHSB(color);
            button.setToolTipText(new TableToolTipBuilder()
                  .addRow(name, ColorUtils.colorToHex(color))
                  .addRow("Rel. lum.", Utils.format("%.4f", ColorUtils.relativeLuminance(color)))
                  .addRow("H", Utils.format("%.4f", hsb[0]))
                  .addRow("S", Utils.format("%.4f", hsb[1]))
                  .addRow("B", Utils.format("%.4f", hsb[2]))
                  .build());
            colorPanel.add(button);
         }
         colorPanel.revalidate();
         colorPanel.repaint();
      };

      List<ColorSorting> colorSortings = List.of(
            new ColorSorting("Name", Comparator.comparing(NamedColor::name)),
            new ColorSorting("Luminance", Comparator.comparingDouble(c -> ColorUtils.relativeLuminance(c.color))),
            new ColorSorting("Hue", Comparator.comparingDouble(c -> toHSB(c.color)[0])),
            new ColorSorting("Saturation", Comparator.comparingDouble(c -> toHSB(c.color)[1])),
            new ColorSorting("Brightness", Comparator.comparingDouble(c -> toHSB(c.color)[2]))
      );

      JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      ButtonGroup buttonGroup = new ButtonGroup();
      for (ColorSorting colorSorting : colorSortings) {
         JToggleButton toggleButton = new JToggleButton(colorSorting.name());
         buttonGroup.add(toggleButton);
         toggleButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
               namedColors.sort(colorSorting.comparator);
               resetColorPanel.run();
            }
         });
         if (buttonsPanel.getComponentCount() == 0) {
            toggleButton.doClick();
         }
         buttonsPanel.add(toggleButton);
      }

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(new JScrollPane(colorPanel));
      mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

      JFrame frame = new JFrame("Color names");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(mainPanel);
      frame.setSize(900, 610);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
   }

   private static float[] toHSB(Color value) {
      return Color.RGBtoHSB(value.getRed(), value.getGreen(), value.getBlue(), null);
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(ColorNamesMain::display);
   }

   private record NamedColor(String name, Color color) {
   }

   private record ColorSorting(String name, Comparator<NamedColor> comparator) {
   }
}
