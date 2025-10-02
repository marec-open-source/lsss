package no.imr.tools.web;

import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

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
      HttpURLConnection urlConnection = (HttpURLConnection) uri.toURL().openConnection();
      urlConnection.setRequestMethod("POST");
      urlConnection.setRequestProperty("Content-Type", contentType);
      urlConnection.setDoOutput(true);
      urlConnection.getOutputStream().write(content);
      urlConnection.setConnectTimeout(15_000);
      urlConnection.setReadTimeout(15_000);
      byte[] responseBytes = urlConnection.getInputStream().readAllBytes();
      int responseCode = urlConnection.getResponseCode();
      if (!isResponseCodeOk(responseCode)) {
         throw new IOException("Response code " + responseCode + " posting to " + uri);
      }
      return responseBytes;
   }

   private static boolean isResponseCodeOk(int responseCode) {
      return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT;
   }

   public static String getMediaType(String file) {
      return switch (FileUtils.getSuffix(file)) {
         case ".css" -> "text/css";
         case ".html" -> "text/html";
         case ".ico" -> "image/x-icon";
         case ".js" -> "application/javascript";
         case ".json" -> APPLICATION_JSON;
         case ".png" -> "image/png";
         case ".svg" -> "image/svg+xml";
         default -> TEXT_PLAIN_UTF_8;
      };
   }
}
