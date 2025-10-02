package no.imr.lsss.framework.wizards.appsetup;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseConnectionManager;
import no.imr.lsss.database.types.JavaDBDatabasePlugin;
import no.imr.lsss.framework.config.application.DatabaseConnectionEditor;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.Utils;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.wizardry.WizardStep;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;

final class DatabaseWizardStep extends WizardStep {
   private final LSSS lsss;
   private final JRadioButton defaultRadioButton = new JRadioButton("Use default database settings");
   private final JRadioButton customRadioButton = new JRadioButton("Specify database type and connection parameters");
   private final JPanel detailsPanel = new JPanel(new BorderLayout());
   private final JavaDBDatabasePlugin javaDBDatabasePlugin;
   private boolean javaDBExists;

   DatabaseWizardStep(LSSS lsss) {
      super("Database", LsssHelp.LSSS_SETUP);

      this.lsss = lsss;

      javaDBDatabasePlugin = Utils.getFirstOrThrow(lsss.getDatabaseManager().getDatabasePlugins(), JavaDBDatabasePlugin.class);
      GuiUtils.createButtonGroup(defaultRadioButton, customRadioButton);

      DatabaseConnectionManager connectionManager = lsss.getDatabaseManager().getGlobalDatabaseConnectionManager();
      if (connectionManager.getDatabaseConnection().isConnected() && connectionManager.getDatabasePlugin() != javaDBDatabasePlugin) {
         customRadioButton.setSelected(true);
      } else {
         defaultRadioButton.setSelected(true);
      }

      ActionListener listener = e -> updateDetailPanel();
      defaultRadioButton.addActionListener(listener);
      customRadioButton.addActionListener(listener);
   }

   @Override
   public JComponent getComponent() {
      GridBag gridBag = new GridBag()
            .configureVerticalBox();

      gridBag.add(new JLabel("""
            <html>
            <h1>Database</h1>
            <p>The database is needed when creating new surveys and for generating database reports.</p>
            """));
      gridBag.add(Box.createVerticalStrut(5));

      Box box = Box.createHorizontalBox();
      box.add(defaultRadioButton);
      box.add(Box.createHorizontalStrut(5));
      box.add(customRadioButton);
      gridBag.add(box);

      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(new JSeparator());
      gridBag.add(Box.createVerticalStrut(10));

      gridBag.add(detailsPanel);

      updateDetailPanel();

      return GuiUtils.createScrollPane(gridBag.getPanel());
   }

   private void updateDetailPanel() {
      JComponent component;
      if (defaultRadioButton.isSelected()) {
         javaDBDatabasePlugin.resetInvalidSettings();
         Path dir = javaDBDatabasePlugin.directory.getFile();
         assert dir != null;
         Path javaDBDir = dir.resolve(javaDBDatabasePlugin.databaseName.getValue());
         javaDBExists = Files.exists(javaDBDir);
         DatabaseConnectionManager connectionManager = lsss.getDatabaseManager().getGlobalDatabaseConnectionManager();
         String connectionText;
         if (connectionManager.getDatabaseConnection().isConnected() && connectionManager.getDatabasePlugin() == javaDBDatabasePlugin) {
            connectionText = "Connected to database!";
         } else {
            connectionText = javaDBExists
                  ? "Directory exists: A connection to the existing database will be made."
                  : "Directory does not exist: A new database will be initialized.";
         }
         component = new JLabel("<html> "
               + "<p style='font-size: larger; margin-top: 10px;'>Database type</p>"
               + "<p>" + javaDBDatabasePlugin.getName().displayName()
               + "</p>"
               + "<p style='font-size: larger; margin-top: 10px;'>Database directory</p>"
               + "<p><code>" + javaDBDir + "</code>"
               + "</p>"
               + "<p style='font-size: larger; margin-top: 10px;'>Connection</p>"
               + "<p>" + connectionText + "</p>"
         );
      } else {
         component = new DatabaseConnectionEditor(lsss).getComponent();
      }
      GuiUtils.replaceContent(detailsPanel, component);
   }

   @Override
   public boolean onNext() {
      DatabaseConnectionManager connectionManager = lsss.getDatabaseManager().getGlobalDatabaseConnectionManager();
      if (defaultRadioButton.isSelected()) {
         if (connectionManager.getDatabaseConnection().isConnected() && connectionManager.getDatabasePlugin() == javaDBDatabasePlugin) {
            return true;
         }
         javaDBDatabasePlugin.setPasswordInitialized(true);
         connectionManager.setDatabasePlugin(javaDBDatabasePlugin);
         if (javaDBExists) {
            return connectionManager.openConnection();
         } else {
            return connectionManager.initializeDatabase();
         }
      } else {
         if (connectionManager.getDatabaseConnection().isConnected()) {
            return true;
         } else {
            JOptionPane.showMessageDialog(getWizard().getDialog(), "Please connect to a database!");
            return false;
         }
      }
   }
}
