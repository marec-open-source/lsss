package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.types.DatabasePlugin;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.DatabaseContent;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * Manages the connection to the database.
 * Contains GUI for connecting to the database.
 */
public final class DatabaseConnectionManager {
   private static final String XML_CONNECTION = "connection";
   private static final String XML_NAME = "name";
   private static final String XML_CONNECTED = "connected";

   private final LSSS lsss;

   private @Nullable DatabasePlugin databasePlugin;
   private final DatabaseConnection databaseConnection = new DatabaseConnection();
   private DatabaseData databaseData = new DatabaseData(databaseConnection);

   private final ChangeManager changeManager = new ChangeManager();

   DatabaseConnectionManager(LSSS lsss, @Nullable DatabasePlugin databasePlugin) {
      this.lsss = lsss;
      this.databasePlugin = databasePlugin;
   }

   private String getCurrentPluginName() {
      return databasePlugin != null ? databasePlugin.getName().persistentName() : "";
   }

   public void setDatabasePlugin(@Nullable DatabasePlugin databasePlugin) {
      closeConnection();
      this.databasePlugin = databasePlugin;
      changeManager.notifyListeners();
   }

   public @Nullable DatabasePlugin getDatabasePlugin() {
      return databasePlugin;
   }

   /**
    * Opens a connection to an existing database. Upgrade checks are done by
    * registered FeaturePlugins.
    */
   public boolean openConnection() {
      lsss.setStartupText("Connecting to database...");

      if (databaseConnection.isConnected()) {
         closeConnection();
      }

      //Before background task since a password dialog may be displayed.
      Configuration configuration = getConfiguration(ConnectionType.CONNECT);
      String connectionUrl = configuration.getProperty(Environment.URL);
      AtomicBoolean ok = new AtomicBoolean(true);

      new WorkerDialog(lsss.getReferenceComponent(), "Connecting to database")
            .setHidden(lsss.hasStartupDialog())
            .setOnError(e -> {
               ok.set(false);
               lsss.showError("Failed to connect to database with URL " + connectionUrl +
                     ".\nPlease check that the connection string points to an existing database.", e);
               closeConnection();
            })
            .startWithoutCancel(() -> {
               Log.global.info("Connecting to database: " + connectionUrl);
               databaseConnection.connect(ConnectionType.CONNECT, configuration, LsssDatabaseUtils.getDatabaseClasses(lsss));
            });

      if (databaseConnection.isConnected()) {
         new WorkerDialog(lsss.getReferenceComponent(), "Upgrading database if necessary")
               .setOnError(e -> {
                  ok.set(false);
                  lsss.showError("Error upgrading database. Please check that you are connected to a valid LSSS database." +
                        "\nIf you connected to an empty database, please try to initialize it first.", e);
                  closeConnection();
               })
               .startWithoutCancel(this::doUpgradeFromPlugins);
      }

      connectionStateChanged();

      return ok.get();
   }

   /**
    * Initializes a new database. Default data are copied into database by
    * registered FeaturePlugins
    */
   public boolean initializeDatabase() {
      if (databaseConnection.isConnected()) {
         closeConnection();
      }

      Configuration configuration = getConfiguration(ConnectionType.INITIALIZE);
      String connectionUrl = configuration.getProperty(Environment.URL);
      AtomicBoolean ok = new AtomicBoolean(true);

      new WorkerDialog(lsss.getReferenceComponent(), "Initializing new database")
            .setOnError(e -> {
               ok.set(false);
               lsss.showError("Failed to initialize database with URL " + connectionUrl + ".", e);
            })
            .startWithoutCancel(() -> {
               Log.global.info("Initializing new database: " + connectionUrl);
               databaseConnection.connect(ConnectionType.INITIALIZE, configuration, LsssDatabaseUtils.getDatabaseClasses(lsss));
               copyDefaultDataFromPlugins(c -> true);
            });

      connectionStateChanged();

      return ok.get();
   }

   /**
    * RJK
    * Create a new empty database. Default data are copied into database by
    * registered FeaturePlugins
    */
   public void createEmptyDatabase() {
      if (databaseConnection.isConnected()) {
         closeConnection();
      }

      Configuration configuration = getConfiguration(ConnectionType.INITIALIZE);
      String connectionUrl = configuration.getProperty(Environment.URL);

      new WorkerDialog(lsss.getReferenceComponent(), "Creating new empty database")
            .setOnError(e -> {
               Log.global.log(Level.WARNING, "Error creating database", e);
               lsss.showError("Failed to create new empty database with URL " + connectionUrl + ".", e);
            })
            .startWithoutCancel(() -> {
               Log.global.info("Creating new empty database: " + connectionUrl);
               databaseConnection.connect(ConnectionType.INITIALIZE, configuration, LsssDatabaseUtils.getDatabaseClasses(lsss));

               Set<Class<? extends BaseDatabaseObject>> databaseClasses = new HashSet<>(LsssDatabaseUtils.getSystemClasses(lsss));
               databaseClasses.add(Nation.class);
               copyDefaultDataFromPlugins(databaseClasses::contains);
            });

      connectionStateChanged();
   }

   private Configuration getConfiguration(ConnectionType connectionType) {
      if (databasePlugin == null) {
         throw new IllegalStateException();
      }
      databasePlugin.askForPasswordIfNecessary();
      Configuration configuration = databasePlugin.getConfiguration(connectionType);

      StringBuilder stringBuilder = new StringBuilder("Database connection properties:");
      Properties properties = configuration.getProperties();
      properties.keySet().stream()
            .filter(key -> !System.getProperties().containsKey(key) && !key.equals(Environment.PASS))
            .sorted()
            .forEach(key -> stringBuilder.append('\n').append(key).append('=').append(properties.get(key)));
      Log.global.config(stringBuilder.toString());
      return configuration;
   }

   public void closeConnection() {
      if (databasePlugin == null || !databaseConnection.isConnected()) {
         return;
      }

      new WorkerDialog(lsss.getReferenceComponent(), "Closing database connection")
            .setOnError(e -> Log.global.log(Level.WARNING, "Error closing database connection", e))
            .startWithoutCancel(() -> {
               Log.global.info("Disconnecting from database");
               databaseConnection.disconnect();
               if (lsss.getLsssConfig().isPrimaryLSSS) {
                  databasePlugin.shutDown();
               }
            });

      connectionStateChanged();
   }

   /**
    * This notifies changes to this particular database connection.
    * Always listen to {@link DatabaseManager#getConnectionChangeManager()} instead of this method
    * to get notified of changes to the active database connection.
    *
    * @return the ChangeManager
    */
   public ChangeManager getChangeManager() {
      return changeManager;
   }

   private void connectionStateChanged() {
      resetDatabaseData();
   }

   public void resetDatabaseData() {
      databaseData = new DatabaseData(databaseConnection);
      changeManager.notifyListeners();
   }

   public DatabaseConnection getDatabaseConnection() {
      return databaseConnection;
   }

   public DatabaseData getDatabaseData() {
      return databaseData;
   }

   public Element toXml() {
      Element connectionElement = DocumentHelper.createElement(XML_CONNECTION)
            .addAttribute(XML_NAME, getCurrentPluginName())
            .addAttribute(XML_CONNECTED, Boolean.toString(databaseConnection.isConnected()));
      if (databasePlugin != null) {
         Element pluginElement = databasePlugin.toXml();
         if (pluginElement != null) {
            connectionElement.add(pluginElement);
         }
      }
      return connectionElement;
   }

   public void fromXml(Element element) {
      if (XmlUtils.equalContent(toXml(), element)) {
         // Same configuration. No need to reconnect.
         return;
      }

      String pluginName = element.attributeValue(XML_NAME);
      boolean connected = Boolean.parseBoolean(element.attributeValue(XML_CONNECTED));
      Element pluginElement = element.elements().isEmpty() ? null : element.elements().getFirst();

      setDatabasePlugin(null);

      for (DatabasePlugin plugin : lsss.getDatabaseManager().getDatabasePlugins()) {
         if (plugin.getName().persistentName().equals(pluginName)) {
            if (pluginElement != null) {
               plugin.fromXml(pluginElement);
            }
            setDatabasePlugin(plugin);
            if (connected) {
               openConnection();
            }
            break;
         }
      }
   }

   private void copyDefaultDataFromPlugins(Predicate<Class<? extends BaseDatabaseObject>> predicate) {
      for (FeaturePlugin plugin : lsss.getPluginManager().getFeaturePlugins()) {
         DatabaseContent databaseContent = plugin.getDatabaseContent();
         if (databaseContent == null) {
            continue;
         }
         databaseContent.copyDefaultDataIntoTables(databaseConnection, predicate);
      }
   }

   private void doUpgradeFromPlugins() throws UpgradeException {
      for (FeaturePlugin plugin : lsss.getPluginManager().getFeaturePlugins()) {
         DatabaseContent databaseContent = plugin.getDatabaseContent();
         if (databaseContent == null) {
            continue;
         }
         DatabaseContent.UpgradeResult result = databaseContent.doUpgradeIfNecessary(databaseConnection, lsss.getFrame(), lsss.getInterpretationSettings().isInteractiveMode());
         switch (result) {
            case OK -> {
            }
            case CANCELLED -> {
               GuiUtils.invokeNowOrWait(() -> {
                  JOptionPane.showMessageDialog(lsss.getReferenceComponent(),
                        "Upgrade of " + plugin.getName().displayName() + " database cancelled.\nFurther use of LSSS without upgrading might lead to corruption of data.",
                        "DB upgrade", JOptionPane.WARNING_MESSAGE);
               });
            }
         }
      }
   }
}
