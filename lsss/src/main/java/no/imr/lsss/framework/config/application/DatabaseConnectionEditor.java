package no.imr.lsss.framework.config.application;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseConnectionManager;
import no.imr.lsss.database.types.DatabasePlugin;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseConnectionEditor {
   private final LSSS lsss;
   private final DatabaseConnectionManager databaseConnectionManager;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JPanel pluginConfigurationPanel = new JPanel(new BorderLayout());
   private final JComboBox<Object> databaseTypeComboBox;
   private final JLabel connectedLabel = new JLabel();
   private final JButton connectButton = new JButton("Connect");
   private final JButton disconnectButton = new JButton("Disconnect");
   private final JButton initializeButton = new JButton("Create and initialize");
   private final JButton createEmptyButton = new JButton("Create empty DB");

   public DatabaseConnectionEditor(LSSS lsss) {
      this.lsss = lsss;
      databaseConnectionManager = lsss.getDatabaseManager().getGlobalDatabaseConnectionManager();

      mainPanel.setBorder(GuiUtils.DEFAULT_MARGIN);

      List<Object> databaseItems = new ArrayList<>();
      databaseItems.add("No connection");
      databaseItems.addAll(lsss.getDatabaseManager().getDatabasePlugins());
      databaseTypeComboBox = new JComboBox<>(databaseItems.toArray());
      databaseTypeComboBox.setSelectedItem(databaseConnectionManager.getDatabasePlugin());
      databaseTypeComboBox.addItemListener(e -> {
         DatabasePlugin databasePlugin = e.getItem() instanceof DatabasePlugin p ? p : null;
         databaseConnectionManager.setDatabasePlugin(databasePlugin);
      });

      Box box = Box.createVerticalBox();
      mainPanel.add(box);
      box.add(createDatabaseTypeSelection());
      box.add(Box.createVerticalStrut(5));
      box.add(pluginConfigurationPanel);
      box.add(Box.createVerticalStrut(10));
      box.add(createButtons());

      WhenShowingListening.connect(mainPanel, databaseConnectionManager.getChangeManager(), GuiListeners.coalescingLater(this::update));
   }

   public JComponent getComponent() {
      update();
      return mainPanel;
   }

   private void update() {
      boolean canEdit = canEdit();
      updateEnabledState();

      if (databaseConnectionManager.getDatabaseConnection().isConnected()) {
         MiscIcons.CHECK.on(connectedLabel).setText("Connected");
         connectedLabel.setBackground(ColorUtils.LIGHTGREEN);
      } else {
         MiscIcons.DELETE.on(connectedLabel).setText("Disconnected");
         connectedLabel.setBackground(ColorUtils.SALMON);
      }

      pluginConfigurationPanel.removeAll();
      DatabasePlugin databasePlugin = databaseConnectionManager.getDatabasePlugin();
      if (databasePlugin != null) {
         databaseTypeComboBox.setSelectedItem(databasePlugin);
         databasePlugin.setGUIEnabled(canEdit && !databaseConnectionManager.getDatabaseConnection().isConnected());
         JPanel panel = new JPanel(new BorderLayout());
         panel.add(databasePlugin.getConfigurationGUI());
         WhenShowingListening.connect(panel, databasePlugin.getParameters(), this::updateEnabledState);
         pluginConfigurationPanel.add(panel);
      } else {
         databaseTypeComboBox.setSelectedIndex(0);
      }
      pluginConfigurationPanel.setBorder(BorderFactory.createEmptyBorder());
      GuiUtils.validateAndRepaintTopmostParent(pluginConfigurationPanel);
   }

   private boolean canEdit() {
      ConfigurationManager configurationManager = lsss.getConfigurationManager();
      return configurationManager.canEdit(UserProfile.ADMINISTRATOR_MODE)
            && !configurationManager.getApplicationConfiguration().getDatabaseConf().useLocalDatabase.getBooleanValue();
   }

   private void updateEnabledState() {
      boolean canEdit = canEdit();
      boolean connected = databaseConnectionManager.getDatabaseConnection().isConnected();
      DatabasePlugin databasePlugin = databaseConnectionManager.getDatabasePlugin();
      boolean configurationValid = databasePlugin != null && databasePlugin.isConfigurationValid();
      boolean canConnect = configurationValid && databasePlugin.canConnect();

      databaseTypeComboBox.setEnabled(canEdit && !connected);
      connectButton.setEnabled(canEdit && !connected && canConnect);
      initializeButton.setEnabled(canEdit && !connected && configurationValid);
      createEmptyButton.setEnabled(canEdit && !connected && configurationValid);
      disconnectButton.setEnabled(canEdit && connected);
   }

   private JComponent createDatabaseTypeSelection() {
      Box box = Box.createHorizontalBox();
      box.add(new JLabel("Database type"));
      box.add(Box.createHorizontalStrut(5));
      box.add(databaseTypeComboBox);
      return box;
   }

   private JComponent createButtons() {
      JLabel infoLabel = new JLabel("""
            <html>
            <body style='margin-bottom: 10px;'>
            <p style='font-size: larger;'>Connect</p>
            <p>
               Connect to an existing database (with database type and database name as specified).
            </p>
            <p style='font-size: larger; margin-top: 10px;'>Create and initialize</p>
            <p>
               Delete content of specified database, then create database tables and fill tables with
               content from the LSSS distribution.
            </p>
            <p style='font-size: larger; margin-top: 10px;'>Create empty DB</p>
            <p>
               Delete content of specified database, then create database tables.
               Some tables are filled, e.g., Nation, ..*Type, ..*System.
            </p>
            """);

      JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

      leftPanel.add(connectedLabel);
      connectedLabel.setOpaque(true);
      connectedLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)));

      JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      rightPanel.add(connectButton);
      connectButton.setToolTipText("Connect to database");
      connectButton.addActionListener(e -> databaseConnectionManager.openConnection());

      rightPanel.add(disconnectButton);
      disconnectButton.setToolTipText("Disconnect from database");
      disconnectButton.addActionListener(e -> databaseConnectionManager.closeConnection());

      rightPanel.add(initializeButton);
      initializeButton.setToolTipText("Create database tables with default content");
      initializeButton.addActionListener(e -> {
         int answer = JOptionPane.showConfirmDialog(mainPanel, """
                     Initialize database?
                     Any existing data will be deleted!
                     """,
               "Confirm", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
         if (answer == JOptionPane.OK_OPTION) {
            databaseConnectionManager.initializeDatabase(); // Create database and fill with default initial content
         }
      });

      rightPanel.add(createEmptyButton);
      createEmptyButton.setToolTipText("Create database tables, with only content in some key tables");
      createEmptyButton.addActionListener(e -> {
         int answer = JOptionPane.showConfirmDialog(mainPanel, """
                     Create empty database?
                     Any existing data will be deleted!

                     Database content has to be loaded from files.
                     Please use 'Create and initialize' if default content should be loaded.
                     """,
               "Confirm", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
         if (answer == JOptionPane.OK_OPTION) {
            databaseConnectionManager.createEmptyDatabase(); // Create empty database (except for a few key-tables)
         }
      });

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(infoLabel, BorderLayout.NORTH);
      panel.add(rightPanel, BorderLayout.EAST);
      panel.add(leftPanel, BorderLayout.WEST);
      return panel;
   }
}
