package no.imr.tools.help;

import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

public record HelpID(
      String id,
      HelpSystemHelpSet helpSet
) {
   @Override
   public String toString() {
      return helpSet.getId() + '/' + id;
   }

   public String getPageId() {
      int i = id.indexOf('/');
      return i < 0 ? id : id.substring(0, i);
   }

   public String getAnchor() {
      int i = id.indexOf('/');
      return i < 0 ? "" : id.substring(i + 1);
   }

   public boolean isValid() {
      return helpSet.getValidIds().contains(getPageId());
   }

   public void show() {
      HelpSystem helpSystem = helpSet.getHelpSystem();
      if (helpSystem == null) {
         Log.global.warning("No help system for help id: " + this);
         return;
      }
      helpSystem.getHelpDisplayer().display(this);
   }

   public void enableHelpKeyOnButton(JButton button) {
      enableHelpOnButton(button);
      GuiUtils.setAccelerator(button, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
   }

   public void enableHelpKeyMenuItem(JMenuItem menuItem) {
      enableHelpOnButton(menuItem);
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
   }

   public void enableHelpOnButton(AbstractButton button) {
      button.addActionListener(_ -> show());
   }
}
