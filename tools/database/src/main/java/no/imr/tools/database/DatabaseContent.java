package no.imr.tools.database;

import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.upgrade.UpgradeException;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.util.List;
import java.util.function.Predicate;

/**
 * Abstract base class for database content, used by LSSS plugins.
 * Implementations take care of database content for plugins,
 * i.e. all knowledge of tables, upgrades, export/import, default values.
 */
public abstract class DatabaseContent {
   protected DatabaseContent() {
   }

   public abstract List<Class<? extends BaseDatabaseObject>> getDatabaseClasses();

   public abstract UpgradeResult doUpgradeIfNecessary(DatabaseConnection databaseConnection, @Nullable Component referenceComponent, boolean interactiveMode) throws UpgradeException;

   public void copyDefaultDataIntoTables(DatabaseConnection databaseConnection) {
      copyDefaultDataIntoTables(databaseConnection, c -> true);
   }

   public abstract void copyDefaultDataIntoTables(DatabaseConnection databaseConnection, Predicate<Class<? extends BaseDatabaseObject>> predicate);

   public enum UpgradeResult {
      OK,
      CANCELLED
   }
}
