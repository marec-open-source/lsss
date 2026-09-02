package no.imr.tools.swing;

import no.imr.tools.misc.TextFilter;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public final class MenuFilter {
   private final JMenu menu;
   private final JPanel panel = new JPanel(new BorderLayout());
   private final JTextField textField = new JTextField(10);

   public MenuFilter(JMenu menu) {
      this.menu = menu;

      panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
      panel.add(new JLabel("Filter: "), BorderLayout.WEST);
      panel.add(textField);

      textField.addKeyListener(new TextFieldKeyListener());
      textField.getDocument().addDocumentListener(new SimpleDocumentListener(_ -> textChanged()));

      menu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuSelected(MenuEvent e) {
            SwingUtilities.invokeLater(textField::requestFocus); // requestFocusInWindow does not work with Alt+W
         }

         @Override
         public void menuDeselected(MenuEvent e) {
            textField.setText("");
         }

         @Override
         public void menuCanceled(MenuEvent e) {
            menuDeselected(e);
         }
      });
   }

   public JComponent getComponent() {
      return panel;
   }

   private void textChanged() {
      TextFilter filter = new TextFilter(textField.getText());
      MenuElement firstVisibleMenuElement = null;
      for (Component component : menu.getPopupMenu().getComponents()) {
         if (component instanceof JMenuItem item) {
            boolean visible = filter.test(item.getText());
            item.setVisible(visible);
            if (visible && firstVisibleMenuElement == null) {
               firstVisibleMenuElement = item;
            }
         }
      }
      menu.getPopupMenu().pack();
      if (!menu.isSelected()) {
         return;
      }
      if (firstVisibleMenuElement != null) {
         MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
         MenuElement[] path = menuSelectionManager.getSelectedPath();
         MenuElement lastElement = path.length > 0 ? path[path.length - 1] : null;
         if (lastElement instanceof JMenuItem item) {
            if (!item.isVisible()) {
               path[path.length - 1] = firstVisibleMenuElement;
               menuSelectionManager.setSelectedPath(path);
            }
         } else if (lastElement == menu.getPopupMenu()) {
            path = Arrays.copyOf(path, path.length + 1);
            path[path.length - 1] = firstVisibleMenuElement;
            menuSelectionManager.setSelectedPath(path);
         }
      }
      textField.requestFocusInWindow();
   }

   private final class TextFieldKeyListener extends KeyAdapter {
      private TextFieldKeyListener() {
      }

      @Override
      public void keyPressed(KeyEvent e) {
         switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER -> {
               MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
               MenuElement[] path = menuSelectionManager.getSelectedPath();
               if (path.length > 0 && path[path.length - 1] instanceof JMenuItem item) {
                  menuSelectionManager.clearSelectedPath();
                  item.doClick(0);
               }
            }
            case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> {
               if (textField.getText().isEmpty()) {
                  menu.dispatchEvent(e);
                  menu.getParent().requestFocusInWindow();
               }
            }
            default -> {
            }
         }
      }
   }
}
