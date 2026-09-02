package no.imr.tools.swing;

import no.imr.tools.Utils;
import no.imr.tools.adm.AdmService;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.adm.LicenseInfo;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Displays a dialog with info about an application.
 */
public final class AboutDialog {
   private AboutDialog() {
   }

   public static void show(@Nullable Window window, ApplicationInfo applicationInfo) {
      LicenseInfo licenseInfo = AdmService.INSTANCE.getLicenseInfo();
      String licenseText = licenseInfo == null ? "" :
            "Licensed to " + licenseInfo.licensedTo() + "<br>"
                  + "License is valid until " + licenseInfo.expiration() + ".<br>"
                  + "Features: " + String.join(", ", licenseInfo.features()) + "<br>"
                  + "<br>";
      String text =
            "<html><body style='text-align: center;'><br>"
                  + "<h2>" + applicationInfo.longName() + "</h2>"
                  + "Version " + applicationInfo.version() + "<br>"
                  + "Built on " + TimeUtils.createUTCDateTimeFormatter("MMMM d, yyyy").format(Utils.BUILD_TIME) + "<br>"
                  + "<br>"
                  + licenseText
                  + "<small>Copyright © " + TimeUtils.createUTCDateTimeFormatter("yyyy").format(Utils.BUILD_TIME)
                  + " NORCE Research AS.</small>";

      JPanel panel = new JPanel(new BorderLayout());
      panel.setBackground(Color.WHITE);
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      panel.add(new JLabel(new ImageIcon(applicationInfo.bigImage())), BorderLayout.NORTH);
      panel.add(new JLabel(text));

      JDialog dialog = new JDialog(window, "About " + applicationInfo.appName());
      if (window != null) {
         dialog.setIconImages(window.getIconImages());
      }
      GuiUtils.setAccelerator(dialog.getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), dialog::dispose);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setResizable(false);
      dialog.getContentPane().add(panel);
      dialog.pack();
      dialog.setLocationRelativeTo(window);
      dialog.setVisible(true);
   }
}
