package no.imr.tools.swing.taskobserver;

import no.imr.tools.adm.AdmService;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.adm.LicenseInfo;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ViewHolder;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * Displays a dialog showing current tasks.
 */
public final class DialogTaskObserver implements TaskObserver {
   private final String title;
   private final ApplicationInfo applicationInfo;
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   public DialogTaskObserver(String title, ApplicationInfo applicationInfo) {
      this.title = title;
      this.applicationInfo = applicationInfo;
      viewHolder.withView(view -> view.dialog.setVisible(true));
   }

   @Override
   public void setPrimaryTask(String text) {
      Log.global.info(text);
      viewHolder.ifView(view -> {
         view.mainLabel.setText(text);
         view.secondaryLabel.setText(null);
      });
   }

   @Override
   public void setSecondaryTask(String text) {
      Log.global.finest(text);
      viewHolder.ifView(view -> {
         view.secondaryLabel.setText(text);
      });
   }

   @Override
   public void done() {
      viewHolder.ifView(view -> {
         view.dialog.dispose();
      });
   }

   private static final class View implements ViewHolder.View {
      private final JDialog dialog;
      private final JLabel mainLabel;
      private final JLabel secondaryLabel;

      private View(DialogTaskObserver dialogTaskObserver) {
         mainLabel = new JLabel(dialogTaskObserver.title, JLabel.LEFT);
         mainLabel.setPreferredSize(new Dimension(300, 0));
         secondaryLabel = new JLabel("...", JLabel.LEFT);

         JPanel topPanel = new JPanel(new BorderLayout());
         topPanel.setBackground(Color.WHITE);
         topPanel.add(new JLabel(new ImageIcon(dialogTaskObserver.applicationInfo.bigImage())));
         LicenseInfo licenseInfo = AdmService.INSTANCE.getLicenseInfo();
         if (licenseInfo != null) {
            topPanel.add(new JLabel("<html><br>Licensed to " + licenseInfo.licensedTo() + "<br><br>", JLabel.CENTER), BorderLayout.SOUTH);
         }

         JPanel labelPanel = new JPanel(new GridLayout(2, 0));
         labelPanel.setBackground(Color.WHITE);
         labelPanel.add(mainLabel);
         labelPanel.add(secondaryLabel);

         JPanel panel = new JPanel(new BorderLayout());
         panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         panel.setBackground(Color.WHITE);
         panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
         panel.add(topPanel, BorderLayout.NORTH);
         panel.add(labelPanel);

         dialog = new JDialog(null, dialogTaskObserver.title, Dialog.ModalityType.DOCUMENT_MODAL);
         dialog.setIconImage(dialogTaskObserver.applicationInfo.image());
         dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
         dialog.getContentPane().add(panel);
         dialog.pack();
         dialog.setLocationRelativeTo(null);
      }

      @Override
      public JComponent getComponent() {
         throw new UnsupportedOperationException();
      }
   }
}
