package no.imr.tools.swing;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.geom.RoundRectangle2D;

public final class Toast {
   private Toast() {
   }

   public static void warning(Component referenceComponent, String text) {
      show(referenceComponent, text, new Color(0xdd7722));
   }

   public static void success(Component referenceComponent, String text) {
      show(referenceComponent, text, new Color(0x6ddb6d));
   }

   private static void show(Component referenceComponent, String text, Color background) {
      JDialog dialog = new JDialog(null, null, Dialog.ModalityType.MODELESS);
      dialog.setAlwaysOnTop(true);
      dialog.setUndecorated(true);
      dialog.setFocusableWindowState(false);
      dialog.setOpacity(0);

      JComponent label = GuiUtils.makeMultiLineLabel(text);
      label.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
      dialog.add(label);
      dialog.getContentPane().setBackground(background);
      dialog.pack();

      Point locationOnScreen = referenceComponent.getLocationOnScreen();
      dialog.setLocation(locationOnScreen.x + referenceComponent.getWidth() - dialog.getWidth() - 15, locationOnScreen.y + 15);
      dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 15, 15));
      dialog.setVisible(true);

      int fadeInDuration = 300;
      int fadeOutDuration = 1000;
      int displayDuration = 3000;

      long startFadeInTime = System.currentTimeMillis();
      long startFadeOutTime = startFadeInTime + fadeInDuration + displayDuration;

      Timer fadeInTimer = new Timer(1, null);
      Timer fadeOutTimer = new Timer(1, null);
      fadeOutTimer.setInitialDelay(displayDuration);

      fadeInTimer.addActionListener(_ -> {
         float opacity = Math.clamp((System.currentTimeMillis() - startFadeInTime) / (float) fadeInDuration, 0, 1);
         dialog.setOpacity(opacity);
         if (opacity >= 1) {
            fadeInTimer.stop();
            fadeOutTimer.start();
         }
      });
      fadeInTimer.start();

      fadeOutTimer.addActionListener(_ -> {
         float opacity = 1 - Math.clamp((System.currentTimeMillis() - startFadeOutTime) / (float) fadeOutDuration, 0, 1);
         dialog.setOpacity(opacity);
         if (opacity <= 0) {
            fadeOutTimer.stop();
            dialog.dispose();
         }
      });
   }
}
