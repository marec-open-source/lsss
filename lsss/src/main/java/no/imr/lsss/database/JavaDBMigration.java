package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseContent;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.HsqldbConnection;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.database.JavaDBConnection;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.QueryBuilder;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.time.TimeUtils;
import no.imr.tools.upgrade.UpgradeException;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

public final class JavaDBMigration {
   private JavaDBMigration() {
   }

   public static boolean interactivelyConvertJavaDBToHsqldb(LSSS lsss, Path dir, String databaseName,
                                                            Supplier<@Nullable Component> referenceComponent,
                                                            String databaseLabel) {
      if (!lsss.getInterpretationSettings().isInteractiveMode()) {
         return false;
      }
      if (!JavaDBUtils.isJavaDBDatabase(dir, databaseName)) {
         return false;
      }
      return GuiUtils.getNowOrWait(() -> {
         String existingFileWarning = HsqldbUtils.doesSomeDatabaseFileExist(dir, databaseName)
               ? """
               <br>
               <p style='color:red;'>
                  NB: Some HSQLDB files already exist and will be overwritten during the conversion.
               </p>
               <br>
               """
               : "";
         int answer = GuiUtils.showOptionDialog(referenceComponent.get(), "Convert JavaDB to HSQLDB?",
               """
                     <html>
                     <p style='color:red;'>
                        WARNING: The\s""" + databaseLabel + """
                        database is of type JavaDB, which is deprecated.
                     </p>
                     <p>
                        Support for JavaDB will be dropped in a future version of LSSS.<br>
                        As an alternative, please use the HSQLDB database type.
                     </p>
                     """
                     + existingFileWarning,
               new String[]{"Convert to HSQLDB", "Not now, keep using JavaDB"});
         if (answer != 0) {
            return false;
         }
         WorkerDialog.Result result = new WorkerDialog(referenceComponent, "Converting JavaDB to HSQLDB...")
               .start(asyncHandle -> convertJavaDBToHsqldb(lsss, dir, databaseName, asyncHandle));
         return result.success();
      });
   }

   public static void convertJavaDBToHsqldb(LSSS lsss, Path dir, String databaseName, AsyncHandle asyncHandle) throws Exception {
      List<Class<? extends BaseDatabaseObject>> databaseClasses = LsssDatabaseUtils.getAllDatabaseClasses(lsss);
      Exception exception = null;
      try (JavaDBConnection source = new JavaDBConnection(ConnectionType.CONNECT, dir, databaseName, databaseClasses)) {
         for (FeaturePlugin plugin : lsss.getPluginManager().getFeaturePlugins()) {
            DatabaseContent databaseContent = plugin.getDatabaseContent();
            if (databaseContent == null) {
               continue;
            }
            DatabaseContent.UpgradeResult upgradeResult = databaseContent.doUpgradeIfNecessary(source.getDatabaseConnection(), null, false);
            if (upgradeResult != DatabaseContent.UpgradeResult.OK) {
               throw new UpgradeException("Database upgrade of " + plugin.getName().displayName() + " not OK");
            }
         }

         try (HsqldbConnection destination = new HsqldbConnection(ConnectionType.INITIALIZE, dir, databaseName, databaseClasses)) {
            for (Class<? extends BaseDatabaseObject> databaseClass : databaseClasses) {
               if (asyncHandle.isCancelled()) {
                  break;
               }
               DatabaseUtils.copyByInsert(source.getDatabaseConnection(), QueryBuilder.fetch(databaseClass).build(), destination.getDatabaseConnection(), asyncHandle);
            }
         }
      } catch (Exception e) {
         exception = e;
      } finally {
         try {
            if (exception == null && !asyncHandle.isCancelled()) {
               // OK => Rename JavaDB.
               String destinationName = databaseName + "-backup-"
                     + TimeUtils.createLocalDateTimeFormatter("yyyy_MM_dd-HH_mm_ss").format(Instant.now());
               Path destination = dir.resolve(destinationName);
               int n = 1;
               while (Files.exists(destination)) {
                  n++;
                  destination = dir.resolve(destinationName + "-" + n);
               }
               Files.move(dir.resolve(databaseName), destination);
            } else {
               // Not OK = Delete Hsqldb.
               HsqldbUtils.deleteDatabaseFiles(dir, databaseName);
            }
         } catch (Exception e) {
            if (exception != null) {
               exception.addSuppressed(e);
            } else {
               exception = e;
            }
         }
         if (exception != null) {
            throw exception;
         }
      }
   }
}
