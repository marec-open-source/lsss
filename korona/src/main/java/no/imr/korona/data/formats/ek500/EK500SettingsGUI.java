package no.imr.korona.data.formats.ek500;

import no.imr.tools.help.HelpID;
import no.imr.tools.parameter.gui.ParameterTableGUI;
import no.imr.tools.parameter.gui.ParameterTableModel;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An editor for {@link EK500Settings}.
 */
public final class EK500SettingsGUI {
   private final Path file;
   private @Nullable HelpID helpID;
   private final JButton okButton = new JButton("OK");

   public EK500SettingsGUI(Path file) {
      this.file = file;
   }

   public EK500SettingsGUI setHelpID(HelpID helpID) {
      this.helpID = helpID;
      return this;
   }

   public void show(@Nullable Component referenceComponent) {
      EK500Settings ek500Settings;
      try {
         ek500Settings = EK500Settings.createFromReferenceLocation(file);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error loading " + file, e);
         return;
      }

      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), "EK500 settings editor (" + file + ")", Dialog.ModalityType.DOCUMENT_MODAL);

      List<EK500TransducerSettings> rows = new ArrayList<>(ek500Settings.getEK500TransducerSettings().values());
      ParameterTableModel<EK500TransducerSettings> parameterTableModel = new ParameterTableModel<>(EK500TransducerSettings::new, rows);
      ParameterTableGUI<EK500TransducerSettings> parameterTableGUI = new ParameterTableGUI<>(parameterTableModel);

      JPanel panel = new JPanel(new BorderLayout());

      panel.add(parameterTableGUI.createScrollPane());
      panel.add(createButtonPanel(dialog, parameterTableModel), BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.add(panel);
      dialog.setSize(800, 300);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   private JPanel createButtonPanel(JDialog dialog, ParameterTableModel<EK500TransducerSettings> parameterTableModel) {
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      okButton.addActionListener(_ -> {
         List<EK500TransducerSettings> rows = parameterTableModel.getRows();
         rows.sort(null);
         EK500Settings ek500Settings = new EK500Settings(rows);
         try {
            ek500Settings.save(file);
         } catch (IOException ioException) {
            GuiUtils.showErrorDialog(dialog, "Error saving " + file, ioException);
            return;
         }
         dialog.dispose();
      });

      JButton newButton = new JButton("Add transducer");
      newButton.addActionListener(_ -> parameterTableModel.addRow());

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(_ -> dialog.dispose());

      buttonPanel.add(newButton);
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);

      if (helpID != null) {
         JButton helpButton = new JButton("Help");
         helpID.enableHelpKeyOnButton(helpButton);
         buttonPanel.add(helpButton);
      }

      return buttonPanel;
   }
}
