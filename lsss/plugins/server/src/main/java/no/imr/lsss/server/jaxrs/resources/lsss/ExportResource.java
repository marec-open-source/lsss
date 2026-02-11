package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.server.pojo.ParameterInfo;
import no.imr.lsss.server.pojo.values.ObjectValue;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.web.WebUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.util.stream.Stream;

public final class ExportResource {
   private final ObjectWriter objectWriter;
   private final StreamingExporter exporter;

   ExportResource(ObjectWriter objectWriter, StreamingExporter exporter) {
      this.objectWriter = objectWriter;
      this.exporter = exporter;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response export() {
      StreamingOutput streamingOutput = output -> exporter.exportToStream(new AsyncHandle(), ProgressHandler.ignore(), output, objectWriter);
      return Response.ok(streamingOutput, exporter.isJson() ? MediaType.APPLICATION_JSON : WebUtils.TEXT_PLAIN_UTF_8)
            .build();
   }

   @GET
   @Path("config/xml")
   @Produces(MediaType.APPLICATION_XML)
   public byte[] getConfig() {
      Element xml = exporter.toXml();
      return XmlUtils.toDefaultBytes(xml);
   }

   @POST
   @Path("config/xml")
   @Consumes(MediaType.APPLICATION_XML)
   public void setConfig(byte[] bytes) throws IOException {
      exporter.fromXml(XmlUtils.readDocument(bytes).getRootElement());
   }

   @GET
   @Path("config/parameter")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ParameterInfo> getParameters() {
      return LsssServerUtils.getParameterInfos(exporter);
   }

   @GET
   @Path("config/parameter/{path: .*}")
   @Produces(MediaType.APPLICATION_JSON)
   public ObjectValue getParameter(@PathParam("path") String path) {
      return LsssServerUtils.getParameterValue(exporter, path);
   }

   @POST
   @Path("config/parameter/{path: .*}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setParameter(@PathParam("path") String path, String value) {
      LsssServerUtils.setParameterValue(exporter, path, value);
   }
}
