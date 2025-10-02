package no.imr.tools.swing.statusbar;

import no.imr.tools.swing.GuiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;

public final class StatusBar {
   private static final int SEPARATOR_SPACE = 5;

   private final Box box = Box.createHorizontalBox();

   public StatusBar() {
   }

   public StatusBar addTopBorder() {
      box.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.SEPARATOR_COLOR));
      return this;
   }

   public StatusBar add(Component component) {
      box.add(component);
      return this;
   }

   public StatusBar add(String label, Component component) {
      add(new JLabel(label));
      return add(component);
   }

   public StatusBar addSpace() {
      return add(createSpace());
   }

   private static Component createSpace() {
      return Box.createHorizontalStrut(SEPARATOR_SPACE);
   }

   public StatusBar addSeparator() {
      return add(createSeparator());
   }

   public static Component createSeparator() {
      JPanel panel = new JPanel(new BorderLayout());
      JSeparator separator = new JSeparator(JSeparator.VERTICAL);
      separator.setForeground(GuiUtils.SEPARATOR_COLOR);
      panel.add(separator);
      panel.setBorder(BorderFactory.createEmptyBorder(0, SEPARATOR_SPACE, 0, SEPARATOR_SPACE));
      return panel;
   }

   public StatusBar addFiller() {
      return add(GuiUtils.createHorizontalFiller());
   }

   public JComponent getComponent() {
      return box;
   }
}
