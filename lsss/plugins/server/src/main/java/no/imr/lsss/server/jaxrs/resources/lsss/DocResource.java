package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import no.marec.tools.jaxrs.JaxRsUtils;

import java.io.IOException;

public final class DocResource {
   DocResource() {
   }

   @GET
   public Response getDoc(@Context Request request, @Context UriInfo uriInfo) throws IOException {
      if (!uriInfo.getPath().endsWith("/")) {
         return redirectToLsssDoc(uriInfo, Response.Status.MOVED_PERMANENTLY);
      }
      return getDocFile(request, uriInfo, "index.html");
   }

   @GET
   @Path("{path: .+\\.(html|js|css|py|svg)}")
   public Response getDocFile(@Context Request request, @Context UriInfo uriInfo, @PathParam("path") String path) throws IOException {
      Response response = JaxRsUtils.getResourceFileIfAvailable(request, "no/imr/lsss/server/jaxrs/resources/files/lsss/doc/", path);
      if (response == null) {
         return redirectToLsssDoc(uriInfo, Response.Status.TEMPORARY_REDIRECT);
      }
      return response;
   }

   @GET
   @Path("{path: .+}")
   public Response getDocCatchAll(@Context UriInfo uriInfo) {
      return redirectToLsssDoc(uriInfo, Response.Status.TEMPORARY_REDIRECT);
   }

   public static Response redirectToLsssDoc(@Context UriInfo uriInfo, Response.Status status) {
      return Response.status(status)
            .location(uriInfo.getAbsolutePathBuilder().replacePath("lsss/doc/").build())
            .build();
   }
}
