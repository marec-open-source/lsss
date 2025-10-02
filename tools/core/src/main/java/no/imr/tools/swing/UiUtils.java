package no.imr.tools.swing;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public final class UiUtils {
   private UiUtils() {
   }

   public static Color labelDisabledForeground() {
      return color("Label.disabledForeground");
   }

   public static Color labelForeground() {
      return color("Label.foreground");
   }

   public static Font labelFont() {
      return font("Label.font");
   }

   public static Color panelBackground() {
      return color("Panel.background");
   }

   public static char passwordFieldEchoChar() {
      return UIManager.get("PasswordField.echoChar") instanceof Character c ? c : '*';
   }

   public static int scrollBarWidth() {
      return UIManager.getInt("ScrollBar.width");
   }

   public static int splitPaneDividerSize() {
      Integer size = (Integer) UIManager.get("SplitPane.dividerSize");
      return size != null ? size : 10; // See BasicSplitPaneUI.installDefaults
   }

   public static Color textFieldBackground(boolean enabled) {
      return color(enabled ? "TextField.background" : "TextField.inactiveBackground");
   }

   public static Color textFieldForeground(boolean enabled) {
      return color(enabled ? "TextField.foreground" : "TextField.inactiveForeground");
   }

   public static Color textFieldSelectionBackground() {
      return color("TextField.selectionBackground");
   }

   private static Color color(String key) {
      return UIManager.getColor(key);
   }

   private static Font font(String key) {
      return UIManager.getFont(key);
   }

   public static void initDefaults() {
      System.setProperty("swing.defaultlaf", UIManager.getCrossPlatformLookAndFeelClassName());
      UIManager.put("SplitPane.continuousLayout", true);
      UIManager.put("SplitPane.dividerSize", 5);
      String[] fonts = {
            "Button.font",                              // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "CheckBox.font",                            // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            //"CheckBoxMenuItem.acceleratorFont",         // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=10]
            "CheckBoxMenuItem.font",                    // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "ComboBox.font",                            // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "DesktopIcon.font",                         // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            //"EditorPane.font",                          // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            //"FormattedTextField.font",                  // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            "InternalFrame.titleFont",                  // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "Label.font",                               // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "List.font",                                // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            //"Menu.acceleratorFont",                     // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=10]
            "Menu.font",                                // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "MenuBar.font",                             // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            //"MenuItem.acceleratorFont",                 // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=10]
            "MenuItem.font",                            // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            //"PasswordField.font",                       // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            "PopupMenu.font",                           // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "ProgressBar.font",                         // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "RadioButton.font",                         // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            //"RadioButtonMenuItem.acceleratorFont",      // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=10]
            "RadioButtonMenuItem.font",                 // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "Slider.font",                              // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "Spinner.font",                             // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "TabbedPane.font",                          // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            //"Table.font",                               // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            //"TableHeader.font",                         // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            //"TextArea.font",                            // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            //"TextField.font",                           // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            //"TextPane.font",                            // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            "TitledBorder.font",                        // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "ToggleButton.font",                        // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            "ToolBar.font",                             // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=bold,size=12]
            //"ToolTip.font",                             // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
            //"Tree.font",                                // javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
      };
      for (String font : fonts) {
         UIManager.put(font, UIManager.getFont(font).deriveFont(Font.PLAIN));
      }
   }
}
