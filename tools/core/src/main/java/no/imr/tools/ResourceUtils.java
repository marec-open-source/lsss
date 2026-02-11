package no.imr.tools;

import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;

import java.io.IOException;
import java.net.URL;

public final class ResourceUtils {
   private ResourceUtils() {
   }

   public static URL getUrl(String resource) {
      URL url = ResourceUtils.class.getClassLoader().getResource(resource);
      if (url == null) {
         throw new IllegalArgumentException("Error locating resource: " + resource);
      }
      return url;
   }

   public static String getString(String resource) {
      try {
         return FileUtils.readAsString(getUrl(resource), Utils.UTF_8);
      } catch (IOException e) {
         throw new IllegalArgumentException("Error reading resource: " + resource, e);
      }
   }

   public static Document getXml(String resource) {
      try {
         return XmlUtils.readDocument(getUrl(resource));
      } catch (IOException e) {
         throw new IllegalArgumentException("Error reading resource: " + resource, e);
      }
   }

   public static <T> T getJson(String resource, Class<T> clazz) {
      try {
         return JsonUtils.readValue(getUrl(resource), clazz);
      } catch (IOException e) {
         throw new IllegalArgumentException("Error reading resource: " + resource, e);
      }
   }
}
