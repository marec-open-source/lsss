package no.marec.tools.help.server.jaxrs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import no.marec.tools.jaxrs.JaxRsUtils;

import java.io.IOException;

@Path("/")
public final class HelpResource {
   public HelpResource() {
   }

   @Path("api")
   public Class<ApiResource> getApi() {
      return ApiResource.class;
   }

   @GET
   public Response getIndexHtml(@Context Request request, @Context UriInfo uriInfo) throws IOException {
      String path = uriInfo.getPath();
      if (!path.endsWith("/")) {
         return Response.status(Response.Status.MOVED_PERMANENTLY)
               .location(uriInfo.getAbsolutePathBuilder().replacePath(path + "/").build())
               .build();
      }
      return getFile(request, "index.html");
   }

   @GET
   @Path("{file}")
   public Response getFile(@Context Request request, @PathParam("file") String file) throws IOException {
      return JaxRsUtils.getResourceFile(request, "no/marec/tools/help/server/resources/build/webapp/", file);
   }
}
