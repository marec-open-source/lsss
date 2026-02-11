package no.imr.korona.viewer;

import no.imr.korona.Korona;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.swing.GeometryListener;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;

public final class KoronaPlayboxDialog {
   private final KoronaPlaybox koronaPlaybox;
   private final JButton editButton = new JButton("Edit modules...");

   public KoronaPlayboxDialog(@Nullable Component referenceComponent, Korona korona, @Nullable Path cfs, SegmentHandle segmentHandle) {
      koronaPlaybox = new KoronaPlaybox(korona);
      SwingUtilities.invokeLater(() -> {
         // After dialog has appeared
         if (cfs != null) {
            koronaPlaybox.loadCfsFile(cfs);
         }
         initEditButton();
         koronaPlaybox.loadRawFile(segmentHandle.getMainFile());
         koronaPlaybox.start();
      });
      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), segmentHandle.getDisplayName() + " - KORONA playbox", Dialog.ModalityType.DOCUMENT_MODAL);
      GeometryListener.startPreferenceSyncing(dialog, new Dimension(900, 600), null, KoronaPlaybox.getPreferences(), "windowGeometry");
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            koronaPlaybox.close();
            dialog.dispose();
         }
      });
      dialog.getContentPane().add(createMainPanel());
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   private void initEditButton() {
      GuiUtils.setAccelerator(editButton, KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
      editButton.setToolTipText("Display module edit dialog");
      ConfigFileSettings configFileSettings = koronaPlaybox.getModuleContainer().getConfigFileSettings();
      Path moduleConfigurationFile = configFileSettings.getModuleConfigurationFile();
      if (moduleConfigurationFile == null) {
         editButton.setEnabled(false);
      } else {
         editButton.addActionListener(_ -> {
            if (koronaPlaybox.editCurrentConfiguration()) {
               try {
                  koronaPlaybox.getModuleContainer().writeConfiguration(moduleConfigurationFile);
                  Path cfsFile = configFileSettings.getFile();
                  if (cfsFile != null) {
                     configFileSettings.save(cfsFile);
                  }
               } catch (IOException ioException) {
                  GuiUtils.showErrorDialog(editButton, "Error saving module setup", ioException);
               }
            }
         });
      }
   }

   private JPanel createMainPanel() {
      JButton helpButton = new JButton("Help");
      helpButton.setToolTipText("Show help on KORONA playbox");
      KoronaHelp.KORONA_PLAYBOX.enableHelpKeyOnButton(helpButton);

      GridBag buttonsGridBag = koronaPlaybox.createButtonsGridBag();
      buttonsGridBag.add(editButton);
      buttonsGridBag.add(helpButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(koronaPlaybox.getComponent());
      mainPanel.add(buttonsGridBag.getPanel(), BorderLayout.NORTH);
      return mainPanel;
   }
}
