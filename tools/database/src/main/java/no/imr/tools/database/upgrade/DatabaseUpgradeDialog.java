package no.imr.tools.database.upgrade;

import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;

public final class DatabaseUpgradeDialog {
   private String sql;
   private boolean ok;

   public DatabaseUpgradeDialog(String sql) {
      this.sql = sql;
   }

   public void show(@Nullable Component referenceComponent) {
      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), "Database upgrade", Dialog.ModalityType.DOCUMENT_MODAL);

      JTextArea textArea = new JTextArea(sql);
      textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

      JButton okButton = new JButton("OK");
      okButton.addActionListener(e -> {
         sql = textArea.getText();
         ok = true;
         dialog.dispose();
      });

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(e -> dialog.dispose());

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.setBorder(BorderFactory.createEtchedBorder());
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);

      JLabel infoLabel = new JLabel("""
            <html>
            <h1>SQL upgrade script</h1>
            This SQL script will upgrade your database to the next version.
            <div style="color: red;">
            <h2>IMPORTANT NOTICE</h2>
            SQL syntax may vary between databases.<br>
            If necessary, please edit the statements according to your database.
            </div>
            """);
      infoLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(5, 5, 20, 5)));

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(infoLabel, BorderLayout.NORTH);
      mainPanel.add(new JScrollPane(textArea));
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.add(mainPanel);
      dialog.setSize(1000, 600);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   public boolean isOK() {
      return ok;
   }

   public String getSQL() {
      return sql;
   }
}
