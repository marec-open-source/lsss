package no.imr.lsss.database.types;

import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.parameter.Name;
import org.hibernate.cfg.Configuration;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class JavaDBFileDatabasePlugin extends NoGuiDatabasePlugin {
   private @Nullable Path dir;
   private @Nullable String databaseName;

   public JavaDBFileDatabasePlugin(Name name) {
      super(name);
   }

   public void setDir(Path dir) {
      this.dir = dir;
   }

   public void setDatabaseName(String databaseName) {
      this.databaseName = databaseName;
   }

   @Override
   public Configuration getConfiguration(ConnectionType connectionType) {
      if (dir == null || databaseName == null) {
         throw new IllegalStateException();
      }
      return JavaDBUtils.createConfiguration(dir, databaseName, connectionType);
   }

   @Override
   public void shutDown() {
      if (dir != null && databaseName != null) {
         JavaDBUtils.shutDown(dir, databaseName);
      }
   }
}
