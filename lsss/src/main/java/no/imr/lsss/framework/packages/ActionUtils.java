package no.imr.lsss.framework.packages;

import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JMenuItem;

public final class ActionUtils {
   private ActionUtils() {
   }

   public static JMenuItem newMenuItem(LsssAction action) {
      return newMenuItem(action, action.getLabel());
   }

   public static JMenuItem newMenuItem(LsssAction action, String text) {
      JMenuItem item = action.getIcon().orElse(MiscIcons.EMPTY).on(new JMenuItem(text));
      prepare(item, action);
      return item;
   }

   public static JMenuItem newCheckboxMenuItem(BooleanLsssAction action) {
      return newCheckboxMenuItem(action, action.getLabel());
   }

   public static JMenuItem newCheckboxMenuItem(BooleanLsssAction action, String text) {
      JMenuItem item = MiscIcons.check(action.get()).on(new JMenuItem(text));
      prepare(item, action);
      WhenShowingListening.connect(item, action.getChangeManager(), GuiListeners.nowOrLater(() -> MiscIcons.check(action.get()).on(item)));
      return item;
   }

   public static JButton newButton(LsssAction action) {
      SvgIcon icon = action.getIcon().orElse(null);
      return icon != null ? newButton(action, icon) : newButton(action, action.getLabel());
   }

   public static JButton newButton(LsssAction action, String text) {
      JButton button = new JButton(text);
      prepare(button, action);
      return button;
   }

   public static JButton newButton(LsssAction action, SvgIcon icon) {
      JButton button = icon.on(new JButton());
      prepare(button, action);
      return button;
   }

   private static void prepare(AbstractButton button, LsssAction action) {
      String toolTipText = action.getToolTipText();
      if (!toolTipText.equals(button.getText())) {
         button.setToolTipText(toolTipText);
      }
      WhenShowingListening.connect(button, action.getChangeManager(), GuiListeners.nowOrLater(() -> button.setEnabled(action.isEnabled())));
      button.setEnabled(action.isEnabled());
      button.addActionListener(e -> action.run(new ActionArgument(e)));
   }
}
