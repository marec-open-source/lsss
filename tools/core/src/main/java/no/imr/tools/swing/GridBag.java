package no.imr.tools.swing;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class GridBag {
   private final JPanel panel;
   private final GridBagConstraints constraints = new GridBagConstraints();
   private boolean nextIsFirstOnLine = true;

   public GridBag() {
      this(new JPanel(new GridBagLayout()));
   }

   public GridBag(JPanel panel) {
      if (!(panel.getLayout() instanceof GridBagLayout)) {
         throw new IllegalArgumentException("Layout: " + panel.getLayout().getClass());
      }
      this.panel = panel;
   }

   public JPanel getPanel() {
      return panel;
   }

   public GridBagConstraints getConstraints() {
      return constraints;
   }

   public boolean isHorizontalFillActive() {
      return constraints.fill == GridBagConstraints.HORIZONTAL || constraints.fill == GridBagConstraints.BOTH;
   }

   public boolean isVerticalFillActive() {
      return constraints.fill == GridBagConstraints.VERTICAL || constraints.fill == GridBagConstraints.BOTH;
   }

   public GridBag activateHorizontalFill() {
      constraints.weightx = 1;
      constraints.fill = isVerticalFillActive() ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
      return this;
   }

   public GridBag activateVerticalFill() {
      constraints.weighty = 1;
      constraints.fill = isHorizontalFillActive() ? GridBagConstraints.BOTH : GridBagConstraints.VERTICAL;
      return this;
   }

   public GridBag deactivateFill() {
      constraints.weightx = 0;
      constraints.fill = GridBagConstraints.NONE;
      return this;
   }

   public GridBag add(Component component) {
      Insets savedInsets = (Insets) constraints.insets.clone();
      if (nextIsFirstOnLine) {
         constraints.insets.left = 0;
      }
      if (constraints.gridwidth == GridBagConstraints.REMAINDER) {
         constraints.insets.right = 0;
      }
      panel.add(component, constraints);
      constraints.insets = savedInsets;
      nextIsFirstOnLine = constraints.gridwidth == GridBagConstraints.REMAINDER;
      return this;
   }

   public GridBag addWithLineBreak(Component component) {
      int savedGridWidth = constraints.gridwidth;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      add(component);
      constraints.gridwidth = savedGridWidth;
      return this;
   }

   public GridBag addWithLineBreak(Component... components) {
      if (components.length == 0) {
         return this;
      }
      for (int i = 0; i < components.length - 1; i++) {
         add(components[i]);
      }
      return addWithLineBreak(components[components.length - 1]);
   }

   public GridBag configureVerticalBox() {
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      constraints.anchor = GridBagConstraints.WEST;
      return activateHorizontalFill();
   }

   public GridBag addVerticalFiller() {
      JLabel verticalFiller = new JLabel();
      int savedFill = constraints.fill;
      constraints.fill = GridBagConstraints.VERTICAL;
      constraints.weighty = 1;
      addWithLineBreak(verticalFiller);
      constraints.fill = savedFill;
      return this;
   }
}
