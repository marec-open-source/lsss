package no.marec.tools.help.server.jaxrs.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.misc.JsonUtils;
import no.marec.tools.help.server.jaxrs.JaxRsApplication;
import no.marec.tools.help.server.pojo.ClientConfig;
import no.marec.tools.help.server.pojo.HelpSet;
import no.marec.tools.jaxrs.JaxRsUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class ApiResource {
   private static final String SRC_MAIN_RESOURCES = "/src/main/resources/";

   private final JaxRsApplication jaxRsApplication;

   @Inject
   public ApiResource(JaxRsApplication jaxRsApplication) {
      this.jaxRsApplication = jaxRsApplication;
   }

   @GET
   @Path("config.json")
   public Response getConfig(@Context Request request) throws IOException {
      Date startTime = Date.from(Utils.START_TIME);
      Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(startTime);
      if (responseBuilder != null) {
         return responseBuilder.build();
      }
      ClientConfig config = new ClientConfig();
      for (String helpDir : jaxRsApplication.getHelpDirs()) {
         String resource = helpDir + "/build/helpSet.json";
         URL url = getResourceUrl(resource);
         if (url == null) {
            Log.global.warning("Cannot find resource " + resource);
            continue;
         }
         HelpSet helpSet = JsonUtils.readValue(url, HelpSet.class);
         config.helpSets.add(helpSet);
      }
      return Response.ok(config, MediaType.APPLICATION_JSON)
            .lastModified(startTime)
            .build();
   }

   @GET
   @Path("lunrIndexes.json")
   public Response getLunrIndex(@Context Request request) throws IOException {
      Date startTime = Date.from(Utils.START_TIME);
      Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(startTime);
      if (responseBuilder != null) {
         return responseBuilder.build();
      }
      List<Object> lunrIndexes = new ArrayList<>();
      for (String helpDir : jaxRsApplication.getHelpDirs()) {
         String resource = helpDir + "/build/lunrIndex.json";
         URL url = getResourceUrl(resource);
         if (url == null) {
            Log.global.warning("Cannot find resource " + resource);
            continue;
         }
         Object lunrIndex = JsonUtils.readValue(url, Object.class);
         lunrIndexes.add(lunrIndex);
      }
      return Response.ok(lunrIndexes, MediaType.APPLICATION_JSON)
            .lastModified(startTime)
            .build();
   }

   @GET
   @Path("file/{path: .*}")
   public Response getFile(@Context Request request, @PathParam("path") String path) throws IOException {
      if (path.contains(SRC_MAIN_RESOURCES)) {
         return JaxRsUtils.getUrl(request, pathToUrl(path));
      } else {
         java.nio.file.Path dir = LoggingManager.getTopInstallationDir();
         java.nio.file.Path file = dir.resolve(FileUtils.toNativeSeparatorChar(path)).normalize();
         if (!file.startsWith(dir)) {
            throw new NotFoundException(path);
         }
         return JaxRsUtils.getFile(request, file);
      }
   }

   private URL pathToUrl(String path) {
      int i = path.indexOf(SRC_MAIN_RESOURCES);
      if (i < 0) {
         throw new NotFoundException(path);
      }
      String resourcePath = path.substring(i + SRC_MAIN_RESOURCES.length());
      URL url = getResourceUrl(resourcePath);
      if (url == null) {
         throw new NotFoundException(path);
      }
      return url;
   }

   private @Nullable URL getResourceUrl(String resource) {
      URL url = ApiResource.class.getClassLoader().getResource(resource);
      if (url != null) {
         return url;
      }
      for (ClassLoader classLoader : jaxRsApplication.getClassLoaders()) {
         url = classLoader.getResource(resource);
         if (url != null) {
            return url;
         }
      }
      return null;
   }
}
