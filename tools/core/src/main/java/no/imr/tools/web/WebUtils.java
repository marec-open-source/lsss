package no.imr.tools.web;

import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class WebUtils {
   public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
   public static final String APPLICATION_JSON = "application/json";
   public static final String APPLICATION_ZIP = "application/zip";
   public static final String TEXT_PLAIN_UTF_8 = "text/plain; charset=UTF-8";
   public static final String TEXT_XML = "text/xml";

   private WebUtils() {
   }

   public static byte[] post(URI uri, byte[] content) throws IOException {
      return post(uri, content, APPLICATION_OCTET_STREAM);
   }

   public static byte[] post(URI uri, byte[] content, String contentType) throws IOException {
      HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofByteArray(content))
            .build();
      HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15));
      Authenticator authenticator = Authenticator.getDefault();
      if (authenticator != null) {
         httpClientBuilder.authenticator(authenticator);
      }
      try (HttpClient httpClient = httpClientBuilder.build()) {
         HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
         if (!isStatusCodeOk(response.statusCode())) {
            throw new IOException("Status code " + response.statusCode() + " posting to " + uri);
         }
         return response.body();
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new IOException("Interrupted posting to " + uri, e);
      }
   }

   private static boolean isStatusCodeOk(int statusCode) {
      return statusCode >= 200 && statusCode < 300;
   }

   public static String getMediaType(String file) {
      return switch (FileUtils.getSuffix(file)) {
         case ".css" -> "text/css";
         case ".html" -> "text/html";
         case ".ico" -> "image/x-icon";
         case ".js" -> "text/javascript";
         case ".json" -> APPLICATION_JSON;
         case ".png" -> "image/png";
         case ".svg" -> "image/svg+xml";
         default -> TEXT_PLAIN_UTF_8;
      };
   }
}
