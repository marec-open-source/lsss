package no.imr.lsss.server.jaxrs.resources.korona;

import com.google.common.util.concurrent.UncheckedExecutionException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import joptsimple.OptionSet;
import no.imr.korona.cli.CliCommand;
import no.imr.korona.cli.CliCommandFactory;
import no.imr.korona.cli.CliCommandJob;
import no.imr.tools.Utils;
import no.imr.tools.web.WebUtils;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KoronaResource {
   public KoronaResource() {
   }

   @GET
   @Path("cli")
   @Produces(MediaType.APPLICATION_JSON)
   public List<Map<String, String>> getCli() {
      return CliCommandFactory.infos().stream()
            .map(info -> {
               Map<String, String> map = new LinkedHashMap<>();
               map.put("name", info.name());
               map.put("description", info.description());
               return map;
            })
            .toList();
   }

   @GET
   @Path("cli/{command}")
   @Consumes(MediaType.WILDCARD)
   public Response getCli(@Context UriInfo uriInfo, @PathParam("command") String commandName) throws IOException {
      return cli(uriInfo, commandName, InputStream.nullInputStream(), false);
   }

   @POST
   @Path("cli/{command}")
   @Consumes(MediaType.WILDCARD)
   public Response postCli(@Context UriInfo uriInfo, @PathParam("command") String commandName, InputStream in) throws IOException {
      return cli(uriInfo, commandName, in, true);
   }

   private static Response cli(UriInfo uriInfo, String commandName, InputStream in, boolean hasInput) throws IOException {
      CliCommand command = CliCommandFactory.create(commandName);
      if (command == null) {
         throw new NotFoundException(commandName);
      }
      String[] args = queryToArgs(uriInfo.getRequestUri().getRawQuery());
      OptionSet options = command.parser.parse(args);
      if (options.has(command.help)) {
         StringWriter writer = new StringWriter();
         writer.append(command.description).append("\n\n");
         command.parser.printHelpOn(writer);
         return Response.ok(writer.toString(), WebUtils.TEXT_PLAIN_UTF_8)
               .build();
      }
      CliCommandJob job = command.createJob(options);

      StreamingOutput streamingOutput;
      if (hasInput) {
         // Do not write response body before completely reading request body
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         cliRun(job, in, byteArrayOutputStream);
         streamingOutput = byteArrayOutputStream::writeTo;
      } else {
         streamingOutput = out -> cliRun(job, in, out);
      }

      return Response.ok(streamingOutput, job.getContentType())
            .build();
   }

   private static void cliRun(CliCommandJob job, InputStream in, OutputStream out) throws IOException {
      PrintStream printStream = new PrintStream(out, false, Utils.UTF_8);
      try {
         job.run(in, printStream);
      } catch (RuntimeException | IOException e) {
         throw e;
      } catch (Exception e) {
         throw new UncheckedExecutionException(e);
      }
      printStream.flush();
   }

   private static String[] queryToArgs(@Nullable String query) {
      if (query == null) {
         return new String[0];
      }
      return Arrays.stream(query.split("&"))
            .map(part -> URLDecoder.decode(part, Utils.UTF_8))
            .toArray(String[]::new);
   }
}
