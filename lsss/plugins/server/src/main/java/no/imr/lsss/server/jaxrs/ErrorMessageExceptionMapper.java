package no.imr.lsss.server.jaxrs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import no.imr.lsss.server.pojo.ErrorMessage;
import no.imr.tools.NoCanDoException;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

@Provider
public final class ErrorMessageExceptionMapper implements ExceptionMapper<Throwable> {
   private final Request request;
   private final UriInfo uriInfo;

   @Inject
   public ErrorMessageExceptionMapper(Request request, UriInfo uriInfo) {
      this.request = request;
      this.uriInfo = uriInfo;
   }

   @Override
   public Response toResponse(Throwable throwable) {
      String id = "#" + ThreadLocalRandom.current().nextInt(1_000_000);
      String message = "Error " + id + ", " + request.getMethod() + " " + uriInfo.getRequestUri() + ", " + throwable;
      Log.global.log(Level.INFO, message, throwable);
      return Response.status(getResponseStatus(throwable))
            .entity(new ErrorMessage(id, Instant.now().toString(), message, Utils.stackTraceToString(throwable)))
            .type(MediaType.APPLICATION_JSON)
            .build();
   }

   private static Response.StatusType getResponseStatus(Throwable throwable) {
      return switch (throwable) {
         case WebApplicationException   e -> e.getResponse().getStatusInfo();
         case FileNotFoundException    __ -> Response.Status.NOT_FOUND;
         case NoSuchFileException      __ -> Response.Status.NOT_FOUND;
         case JsonParseException       __ -> Response.Status.BAD_REQUEST;
         case MismatchedInputException __ -> Response.Status.BAD_REQUEST;
         case NoCanDoException         __ -> Response.Status.BAD_REQUEST;
         default                          -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }
}
