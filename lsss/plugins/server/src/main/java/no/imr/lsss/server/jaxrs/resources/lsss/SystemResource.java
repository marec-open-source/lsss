package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;
import no.imr.lsss.server.pojo.GraphicsInfo;
import no.imr.tools.misc.ThreadDump;
import no.imr.tools.web.WebUtils;

import java.util.Properties;

public final class SystemResource {
   SystemResource() {
   }

   @Path("api-calls")
   public Class<ApiCallsResource> getApiCalls() {
      return ApiCallsResource.class;
   }

   @GET
   @Path("graphics")
   @Produces(MediaType.APPLICATION_JSON)
   public GraphicsInfo getGraphicsInfo() {
      return new GraphicsInfo();
   }

   @GET
   @Path("properties")
   @Produces(MediaType.APPLICATION_JSON)
   public Properties getProperties() {
      return System.getProperties();
   }

   @GET
   @Path("thread-dump")
   @Produces(WebUtils.TEXT_PLAIN_UTF_8)
   public StreamingOutput getThreadDump() {
      return ThreadDump::print;
   }
}
