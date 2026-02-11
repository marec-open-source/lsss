package no.imr.tools.swing;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A tabbed pane with radio buttons as tab components.
 */
public final class RadioButtonTabbedPane {
   private final JTabbedPane tabbedPane = new JTabbedPane();
   private final ButtonGroup buttonGroup = new ButtonGroup();
   private final List<JRadioButton> radioButtons = new ArrayList<>();

   public RadioButtonTabbedPane() {
      tabbedPane.addChangeListener(_ -> {
         radioButtons.get(tabbedPane.getSelectedIndex()).setSelected(true);
      });
   }

   public JTabbedPane getTabbedPane() {
      return tabbedPane;
   }

   public RadioButtonTabbedPane add(String title, Component component) {
      JRadioButton radioButton = new JRadioButton(title);
      radioButton.setOpaque(false);
      radioButton.setFocusable(false);
      radioButtons.add(radioButton);
      buttonGroup.add(radioButton);
      int tabIndex = tabbedPane.getTabCount();
      tabbedPane.addTab(title, component);
      tabbedPane.setTabComponentAt(tabIndex, radioButton);
      if (tabIndex == 0) {
         radioButton.setSelected(true);
      }
      radioButton.addItemListener(e -> {
         if (e.getStateChange() == ItemEvent.SELECTED) {
            tabbedPane.setSelectedIndex(tabIndex);
         }
      });
      return this;
   }
}
