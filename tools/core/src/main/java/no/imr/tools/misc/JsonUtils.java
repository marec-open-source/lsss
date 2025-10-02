package no.imr.tools.misc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.MapType;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
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
         .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
         .defaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.FAIL, Nulls.FAIL))
         .serializationInclusion(JsonInclude.Include.NON_NULL)
         .defaultPrettyPrinter(new DefaultPrettyPrinter()
               .withObjectIndenter(new DefaultIndenter().withLinefeed("\n")))
         .build();

   public static final ObjectWriter PRETTY_PRINTER = JSON_MAPPER.writerWithDefaultPrettyPrinter();

   private JsonUtils() {
   }

   public static <T> T readValue(Path file, Class<T> clazz) throws IOException {
      return JSON_MAPPER.readValue(file.toFile(), clazz);
   }

   public static void writeValue(Path file, Object value, boolean pretty) throws IOException {
      if (pretty) {
         writeValuePrettily(file, value);
      } else {
         writeValueCompactly(file, value);
      }
   }

   public static void writeValuePrettily(Path file, Object value) throws IOException {
      byte[] bytes = PRETTY_PRINTER.writeValueAsBytes(value);
      FileUtils.replaceFileSafely(file, bytes);
   }

   public static void writeValueCompactly(Path file, Object value) throws IOException {
      byte[] bytes = JSON_MAPPER.writeValueAsBytes(value);
      FileUtils.replaceFileSafely(file, bytes);
   }

   public static <T> Set<T> parseSet(String json, Class<T> clazz) throws IOException {
      return JSON_MAPPER.readValue(json, JSON_MAPPER.getTypeFactory().constructCollectionType(LinkedHashSet.class, clazz));
   }

   public static Map<String, Object> parseMap(String json) throws IOException {
      return JSON_MAPPER.readValue(json, mapType());
   }

   public static Map<String, Object> convertToMap(Object value) {
      return JSON_MAPPER.convertValue(value, mapType());
   }

   public static MapType mapType() {
      return JSON_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);
   }
}
