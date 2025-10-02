package no.imr.tools.swing;

import no.imr.tools.Utils;
import no.imr.tools.adm.AdmService;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.adm.LicenseInfo;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.ZoneOffset;

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
                  + "Built on " + Utils.createUTCDateTimeFormatter("MMMM d, yyyy").format(Utils.BUILD_TIME) + "<br>"
                  + "<br>"
                  + licenseText
                  + "<small>Copyright © " + LocalDate.ofInstant(Utils.BUILD_TIME, ZoneOffset.UTC).getYear()
                  + " NORCE Research AS.</small>";

      JPanel panel = new JPanel(new BorderLayout());
      panel.setBackground(Color.WHITE);
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      panel.add(new JLabel(new ImageIcon(applicationInfo.bigImage())), BorderLayout.NORTH);
      panel.add(new JLabel(text));

      JDialog dialog = new JDialog(window, "About " + applicationInfo.appName());
      dialog.addKeyListener(new KeyAdapter() {
         @Override
         public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
               dialog.dispose();
            }
         }
      });
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setResizable(false);
      dialog.getContentPane().add(panel);
      dialog.pack();
      dialog.setLocationRelativeTo(window);
      dialog.setVisible(true);
   }
}
