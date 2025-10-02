package no.imr.lsss.server.jaxrs.resources;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import no.imr.lsss.LSSS;
import no.imr.lsss.server.jaxrs.JaxRsApplication;
import no.imr.lsss.server.jaxrs.resources.internal.InternalResource;
import no.imr.lsss.server.jaxrs.resources.korona.KoronaResource;
import no.imr.lsss.server.jaxrs.resources.lsss.DocResource;
import no.imr.lsss.server.jaxrs.resources.lsss.LsssResource;

@Path("/")
@Singleton
public final class RootResource {
   private final LSSS lsss;
   private final LsssResource lsssResource;

   @Inject
   public RootResource(JaxRsApplication jaxRsApplication) {
      lsss = jaxRsApplication.getLSSS();
      lsssResource = new LsssResource(jaxRsApplication);
   }

   @GET
   public Response getRoot(@Context UriInfo uriInfo) {
      return getRootFile(uriInfo);
   }

   @GET
   @Path("{file: \\w*\\.html}")
   public Response getRootFile(@Context UriInfo uriInfo) {
      return DocResource.redirectToLsssDoc(uriInfo, Response.Status.TEMPORARY_REDIRECT);
   }

   @GET
   @Path("favicon.ico")
   public Response getFavIcon() {
      return Response.status(Response.Status.NOT_FOUND)
            .build();
   }

   @Path("internal")
   public InternalResource getInternalResource() {
      return new InternalResource(lsss);
   }

   @Path("korona")
   public KoronaResource getKoronaResource() {
      return new KoronaResource();
   }

   @Path("lsss")
   public LsssResource getLsssResource() {
      return lsssResource;
   }
}
