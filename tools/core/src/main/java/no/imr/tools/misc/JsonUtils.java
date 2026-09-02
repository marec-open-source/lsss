package no.imr.tools.misc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import no.imr.tools.io.FileUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.MapType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * JSON utilities.
 */
public final class JsonUtils {
   public static final JsonMapper JSON_MAPPER = JsonMapper.builder()
         .changeDefaultNullHandling(_ -> JsonSetter.Value.forValueNulls(Nulls.SKIP, Nulls.SKIP))
         .changeDefaultPropertyInclusion(_ -> JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
         .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
         .defaultPrettyPrinter(new DefaultPrettyPrinter()
               .withObjectIndenter(new DefaultIndenter().withLinefeed("\n")))
         .build();

   public static final ObjectWriter PRETTY_PRINTER = JSON_MAPPER.writerWithDefaultPrettyPrinter();

   private JsonUtils() {
   }

   public static <T> T readValue(URL url, Class<T> clazz) throws IOException {
      try (InputStream in = url.openStream()) {
         return JSON_MAPPER.readValue(in, clazz);
      } catch (JacksonException e) {
         throw new IOException("Error reading " + url, e);
      }
   }

   public static void writeValue(Path file, Object value, boolean pretty) throws IOException {
      if (pretty) {
         writeValuePrettily(file, value);
      } else {
         writeValueCompactly(file, value);
      }
   }

   public static void writeValuePrettily(Path file, Object value) throws IOException {
      try {
         byte[] bytes = PRETTY_PRINTER.writeValueAsBytes(value);
         FileUtils.replaceFileSafely(file, bytes);
      } catch (JacksonException e) {
         throw new IOException(e);
      }
   }

   public static void writeValueCompactly(Path file, Object value) throws IOException {
      try {
         byte[] bytes = JSON_MAPPER.writeValueAsBytes(value);
         FileUtils.replaceFileSafely(file, bytes);
      } catch (JacksonException e) {
         throw new IOException(e);
      }
   }

   public static <T> Set<T> parseSet(String json, Class<T> clazz) {
      return JSON_MAPPER.readValue(json, JSON_MAPPER.getTypeFactory().constructCollectionType(LinkedHashSet.class, clazz));
   }

   public static Map<String, Object> parseMap(String json) {
      return JSON_MAPPER.readValue(json, mapType());
   }

   public static Map<String, Object> convertToMap(Object value) {
      return JSON_MAPPER.convertValue(value, mapType());
   }

   public static MapType mapType() {
      return JSON_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);
   }
}
