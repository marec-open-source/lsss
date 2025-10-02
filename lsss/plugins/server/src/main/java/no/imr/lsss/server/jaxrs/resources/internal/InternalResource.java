package no.imr.lsss.server.jaxrs.resources.internal;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import no.imr.lsss.LSSS;
import no.marec.tools.jaxrs.JaxRsUtils;

import java.io.IOException;

public final class InternalResource {
   private final LSSS lsss;

   public InternalResource(LSSS lsss) {
      this.lsss = lsss;
   }

   @GET
   @Path("{file: [\\w-/]+\\.(html|js|css|py)}")
   public Response getFile(@Context Request request, @PathParam("file") String file) throws IOException {
      return JaxRsUtils.getResourceFile(request, "no/imr/lsss/server/jaxrs/resources/files/internal/", file);
   }

   @Path("debug")
   public DebugResource getDebugResource() {
      return new DebugResource(lsss);
   }

   @POST
   @Path("error")
   public Response error() {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .build();
   }
}
