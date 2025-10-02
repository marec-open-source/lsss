package no.imr.tools.swing;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionListener;

public final class MenuUtils {
   private MenuUtils() {
   }

   public static JMenuItem addItem(JMenu menu, String text, ActionListener actionListener) {
      JMenuItem item = menu.add(text);
      item.addActionListener(actionListener);
      return item;
   }

   public static JMenuItem addItem(JMenu menu, String text, int mnemonic, ActionListener actionListener) {
      JMenuItem item = addItem(menu, text, actionListener);
      item.setMnemonic(mnemonic);
      return item;
   }

   public static JMenu addMenu(JMenu menu, String text, int mnemonic) {
      JMenu subMenu = new JMenu(text);
      menu.add(subMenu);
      subMenu.setMnemonic(mnemonic);
      return subMenu;
   }

   public static JMenu multiColumn(JMenu menu) {
      MultiColumnLayout multiColumnLayout = new MultiColumnLayout();
      multiColumnLayout.setHorizontalFill(true);
      menu.getPopupMenu().setLayout(multiColumnLayout);
      menu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuSelected(MenuEvent e) {
            GraphicsConfiguration graphicsConfiguration = menu.getGraphicsConfiguration();
            if (graphicsConfiguration != null) {
               int height = graphicsConfiguration.getBounds().height;
               multiColumnLayout.setPreferredMaxHeight(height);
               menu.getPopupMenu().pack();
            }
         }
      });
      return menu;
   }
}
