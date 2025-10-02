package no.marec.tools.help.server.jaxrs;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import no.imr.tools.logging.Log;
import no.imr.tools.web.WebUtils;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
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
      String responseMessage = throwable.toString();
      Response.StatusType statusType = getResponseStatus(throwable);
      if (statusType.getFamily() == Response.Status.Family.SERVER_ERROR) {
         String logMessage = "Error " + request.getMethod() + " " + uriInfo.getRequestUri() + ", " + responseMessage;
         Log.global.log(Level.WARNING, logMessage, throwable);
      }
      return Response.status(statusType)
            .entity(responseMessage)
            .type(WebUtils.TEXT_PLAIN_UTF_8)
            .build();
   }

   private static Response.StatusType getResponseStatus(Throwable throwable) {
      return switch (throwable) {
         case WebApplicationException e -> e.getResponse().getStatusInfo();
         case FileNotFoundException  __ -> Response.Status.NOT_FOUND;
         case NoSuchFileException    __ -> Response.Status.NOT_FOUND;
         default                        -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }
}
