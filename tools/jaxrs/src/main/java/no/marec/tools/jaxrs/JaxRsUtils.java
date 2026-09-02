package no.marec.tools.jaxrs;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import no.imr.tools.web.WebUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

public final class JaxRsUtils {
   private JaxRsUtils() {
   }

   public static boolean skipStackTrace(Throwable throwable, Response.StatusType statusType) {
      return statusType.getFamily() != Response.Status.Family.SERVER_ERROR
            || throwable instanceof SocketTimeoutException
            || throwable.getClass().getSimpleName().equals("ClientAbortException");
   }

   public static Response getFile(Request request, Path file) throws IOException {
      BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
      Date lastModified = new Date(fileAttributes.lastModifiedTime().toMillis());
      Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModified);
      if (responseBuilder != null) {
         return responseBuilder.build();
      }
      return Response.ok(file.toFile(), WebUtils.getMediaType(file.getFileName().toString()))
            .lastModified(lastModified)
            .header(HttpHeaders.CONTENT_LENGTH, fileAttributes.size())
            .build();
   }

   public static Response getResourceFile(Request request, String resourceDir, String path) throws IOException {
      Response response = getResourceFileIfAvailable(request, resourceDir, path);
      if (response == null) {
         throw new NotFoundException(path);
      }
      return response;
   }

   public static @Nullable Response getResourceFileIfAvailable(Request request, String resourceDir, String path) throws IOException {
      URL url = JaxRsUtils.class.getClassLoader().getResource(resourceDir + path);
      if (url == null) {
         return null;
      }
      return getUrl(request, url);
   }

   public static Response getUrl(Request request, URL url) throws IOException {
      Date lastModified;
      byte[] entity;
      URLConnection connection = url.openConnection();
      try (InputStream in = connection.getInputStream()) {
         long lastModifiedMillis = connection.getLastModified();
         if (lastModifiedMillis > 0) {
            lastModified = new Date(lastModifiedMillis);
            Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModified);
            if (responseBuilder != null) {
               return responseBuilder.build();
            }
         } else {
            lastModified = null;
         }
         entity = in.readAllBytes();
      }
      return Response.ok(entity, WebUtils.getMediaType(url.getPath()))
            .lastModified(lastModified)
            .build();
   }
}
