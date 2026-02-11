package no.imr.tools.misc;

import tools.jackson.core.JsonGenerator;

import java.util.Collection;
import java.util.function.Consumer;

public final class JsonWriter {
   private final JsonGenerator json;

   public JsonWriter(JsonGenerator json) {
      this.json = json;
   }

   public void writeObjectField(String fieldName, Runnable contentWriter) {
      json.writeName(fieldName);
      writeObject(contentWriter);
   }

   public void writeObject(Runnable contentWriter) {
      json.writeStartObject();
      contentWriter.run();
      json.writeEndObject();
   }

   public <T> void writeArrayField(String fieldName, Collection<T> values, Consumer<T> contentWriter) {
      json.writeName(fieldName);
      writeArray(values, contentWriter);
   }

   public void writeArray(Runnable contentWriter) {
      json.writeStartArray();
      contentWriter.run();
      json.writeEndArray();
   }

   public <T> void writeArray(Collection<T> values, Consumer<T> contentWriter) {
      json.writeStartArray();
      for (T value : values) {
         contentWriter.accept(value);
      }
      json.writeEndArray();
   }
}
