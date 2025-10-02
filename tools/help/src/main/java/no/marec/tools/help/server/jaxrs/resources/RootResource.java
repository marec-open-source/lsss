package no.marec.tools.help.server.jaxrs.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import no.imr.tools.help.HelpSystemInfo;
import no.marec.tools.help.server.jaxrs.JaxRsApplication;

@Path("/")
public final class RootResource {
   private final JaxRsApplication jaxRsApplication;

   @Inject
   public RootResource(JaxRsApplication jaxRsApplication) {
      this.jaxRsApplication = jaxRsApplication;
   }

   @Path("help/{app}/{version}")
   public Class<HelpResource> getApi(@PathParam("app") String app, @PathParam("version") String version) {
      HelpSystemInfo info = jaxRsApplication.getHelpSystemInfo();
      if (!app.equals(info.app()) || !version.equals(info.version())) {
         throw new BadRequestException("Wrong application: Expected " + info.app() + "/" + info.version() + ", but got " + app + "/" + version);
      }
      return HelpResource.class;
   }
}
