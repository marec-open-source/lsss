package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.imr.lsss.database.DatabaseManager;
import no.imr.lsss.framework.config.application.DatabaseConf;
import no.imr.lsss.server.pojo.values.BooleanValue;
import no.imr.lsss.server.util.LsssServerUtils;

public final class DatabaseConfResource extends ConfigurationUnitResource {
   private final DatabaseManager databaseManager;

   DatabaseConfResource(DatabaseConf databaseConf) {
      super(databaseConf);

      databaseManager = databaseConf.getLSSS().getDatabaseManager();
   }

   @GET
   @Path("connected")
   @Produces(MediaType.APPLICATION_JSON)
   public BooleanValue connected() {
      throwIfSurveyLocal();
      return new BooleanValue(databaseManager.getConnectionManager().getDatabaseConnection().isConnected());
   }

   @POST
   @Path("connected")
   public void connected(BooleanValue connected) {
      throwIfSurveyLocal();
      LsssServerUtils.doInGuiThread(() -> {
         if (connected.value) {
            databaseManager.getConnectionManager().openConnection();
         } else {
            databaseManager.getConnectionManager().closeConnection();
         }
      });
   }

   @POST
   @Path("create")
   public void create(@QueryParam("empty") boolean empty) {
      throwIfSurveyLocal();
      if (databaseManager.getConnectionManager().getDatabaseConnection().isConnected()) {
         throw new BadRequestException("Already connected to a database");
      }
      LsssServerUtils.doInGuiThread(() -> {
         if (empty) {
            databaseManager.getConnectionManager().createEmptyDatabase();
         } else {
            databaseManager.getConnectionManager().initializeDatabase();
         }
      });
   }

   private void throwIfSurveyLocal() {
      if (databaseManager.isUseSurveyLocalDatabase()) {
         throw new BadRequestException("Not available when using survey local database");
      }
   }
}
