package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.packages.PackagesConf;
import no.imr.lsss.framework.config.application.packages.UserDefinedAction;
import no.imr.lsss.framework.config.application.packages.UserDefinedPackage;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.framework.packages.LsssPackage;
import no.imr.lsss.server.pojo.ApiLsssAction;
import no.imr.lsss.server.pojo.ApiLsssPackage;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.marec.tools.jaxrs.JaxRsUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

public final class PackageResource {
   private final LSSS lsss;

   PackageResource(LSSS lsss) {
      this.lsss = lsss;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ApiLsssPackage> getPackages(@QueryParam("userDefined") @Nullable Boolean userDefined, @QueryParam("actions") boolean actions) {
      return lsss.getPackageManager().getPackages().stream()
            .filter(p -> userDefined == null || userDefined == (p.getUserDefinedPackage() != null))
            .sorted(Utils.comparingIgnoringCase(LsssPackage::getId))
            .map(p -> {
               ApiLsssPackage apiLsssPackage = new ApiLsssPackage(p);
               if (actions) {
                  apiLsssPackage.actions = getApiLsssActions(p).toList();
               }
               return apiLsssPackage;
            });
   }

   @GET
   @Path("{packageId}")
   public Response getPackage(@PathParam("packageId") String packageId) {
      UserDefinedPackage userDefinedPackage = getUserDefinedPackage(packageId);
      return LsssServerUtils.getZip(userDefinedPackage.getDir());
   }

   @POST
   @Path("{packageId}")
   @Consumes(MediaType.WILDCARD)
   public void savePackage(@PathParam("packageId") String packageId, InputStream in) throws IOException {
      PackagesConf packagesConf = lsss.getConfigurationManager().getAppMiscConf().getPackagesConf();
      LsssServerUtils.saveZip(in, packagesConf.getPackagesDir(), packageId);
      packagesConf.reloadPackages();
   }

   @DELETE
   @Path("{packageId}")
   public void deletePackage(@PathParam("packageId") String packageId) {
      UserDefinedPackage userDefinedPackage = getUserDefinedPackage(packageId);
      userDefinedPackage.getPackagesConf().deleteUserDefinedPackage(userDefinedPackage);
   }

   @GET
   @Path("{packageId}/action")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ApiLsssAction> getActions(@PathParam("packageId") String packageId) {
      return getApiLsssActions(getLsssPackage(packageId));
   }

   @GET
   @Path("{packageId}/action/{actionId}")
   public Response getAction(@PathParam("packageId") String packageId, @PathParam("actionId") String actionId) {
      UserDefinedPackage userDefinedPackage = getUserDefinedPackage(packageId);
      UserDefinedAction userDefinedAction = getUserDefinedAction(userDefinedPackage, actionId);
      return LsssServerUtils.getZip(userDefinedAction.getDir());
   }

   @POST
   @Consumes(MediaType.WILDCARD)
   @Path("{packageId}/action/{actionId}")
   public void saveAction(@PathParam("packageId") String packageId, @PathParam("actionId") String actionId, InputStream in) throws IOException {
      UserDefinedPackage userDefinedPackage = getUserDefinedPackage(packageId);
      LsssServerUtils.saveZip(in, userDefinedPackage.getActionsDir(), actionId);
      userDefinedPackage.getPackagesConf().reloadPackages();
   }

   @DELETE
   @Path("{packageId}/action/{actionId}")
   public void deleteAction(@PathParam("packageId") String packageId, @PathParam("actionId") String actionId) {
      UserDefinedPackage userDefinedPackage = getUserDefinedPackage(packageId);
      UserDefinedAction userDefinedAction = getUserDefinedAction(userDefinedPackage, actionId);
      userDefinedPackage.deleteUserDefinedAction(userDefinedAction);
   }

   @POST
   @Path("{packageId}/action/{actionId}/run")
   public void runAction(@PathParam("packageId") String packageId, @PathParam("actionId") String actionId, @Nullable Map<String, Object> arguments) {
      LsssPackage lsssPackage = getLsssPackage(packageId);
      LsssAction action = lsssPackage.getAction(actionId);
      if (action == null) {
         throw new NotFoundException(actionId);
      }
      action.run(new ActionArgument(arguments != null ? arguments : Map.of()));
   }

   @GET
   @Path("{packageId}/file/{file: .+}")
   public Response getFile(@Context Request request, @PathParam("packageId") String packageId, @PathParam("file") String file) throws IOException {
      UserDefinedPackage userDefinedPackage = getUserDefinedPackage(packageId);
      return JaxRsUtils.getFile(request, userDefinedPackage.getDir().resolve(FileUtils.toNativeSeparatorChar(file)));
   }

   private LsssPackage getLsssPackage(String packageId) {
      LsssPackage lsssPackage = lsss.getPackageManager().getPackage(packageId);
      if (lsssPackage == null) {
         throw new NotFoundException(packageId);
      }
      return lsssPackage;
   }

   private UserDefinedPackage getUserDefinedPackage(String packageId) {
      LsssPackage lsssPackage = getLsssPackage(packageId);
      UserDefinedPackage userDefinedPackage = lsssPackage.getUserDefinedPackage();
      if (userDefinedPackage == null) {
         throw new BadRequestException("Not applicable to built-in packages");
      }
      return userDefinedPackage;
   }

   private static UserDefinedAction getUserDefinedAction(UserDefinedPackage userDefinedPackage, String actionId) {
      return userDefinedPackage.getActions().stream()
            .filter(action -> action.id.equals(actionId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(actionId));
   }

   private static Stream<ApiLsssAction> getApiLsssActions(LsssPackage lsssPackage) {
      return lsssPackage.getActions().stream()
            .sorted(Utils.comparingIgnoringCase(LsssAction::getId))
            .map(ApiLsssAction::new);
   }
}
