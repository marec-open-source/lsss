package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.resources.LsssResource;
import no.imr.tools.adm.AdmService;
import no.imr.tools.adm.LicenseInfo;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;

public final class StartupDialog {
   private final JDialog dialog;
   private final JLabel textLabel = new JLabel("Initializing...", JLabel.CENTER);

   public StartupDialog(JFrame frame) {
      textLabel.setPreferredSize(new Dimension(300, 50));

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.setBackground(Color.WHITE);
      topPanel.add(new JLabel(new ImageIcon(LsssResource.LSSS_64)));
      LicenseInfo licenseInfo = AdmService.INSTANCE.getLicenseInfo();
      if (licenseInfo != null) {
         JLabel licencedLabel = new JLabel("Licensed to " + licenseInfo.licensedTo(), JLabel.CENTER);
         licencedLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
         topPanel.add(licencedLabel, BorderLayout.SOUTH);
      }

      JPanel panel = new JPanel(new BorderLayout());
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      panel.setBackground(Color.WHITE);
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      panel.add(topPanel, BorderLayout.NORTH);
      panel.add(textLabel);

      dialog = new JDialog(frame, "Starting LSSS " + LSSS.VERSION, Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.getContentPane().add(panel);
      dialog.pack();
      dialog.setLocationRelativeTo(frame);
   }

   public void show() {
      dialog.setVisible(true);
   }

   public void setText(String text) {
      textLabel.setText(text);
   }

   public void dispose() {
      dialog.dispose();
   }
}
