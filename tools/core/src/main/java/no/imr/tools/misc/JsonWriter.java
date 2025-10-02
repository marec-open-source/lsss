package no.imr.tools.misc;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Collection;

public final class JsonWriter {
   private final JsonGenerator json;

   public JsonWriter(JsonGenerator json) {
      this.json = json;
   }

   public void writeObjectField(String fieldName, ThrowingRunnable<IOException> contentWriter) throws IOException {
      json.writeFieldName(fieldName);
      writeObject(contentWriter);
   }

   public void writeObject(ThrowingRunnable<IOException> contentWriter) throws IOException {
      json.writeStartObject();
      contentWriter.run();
      json.writeEndObject();
   }

   public <T> void writeArrayField(String fieldName, Collection<T> values, ThrowingConsumer<T, IOException> contentWriter) throws IOException {
      json.writeFieldName(fieldName);
      writeArray(values, contentWriter);
   }

   public void writeArray(ThrowingRunnable<IOException> contentWriter) throws IOException {
      json.writeStartArray();
      contentWriter.run();
      json.writeEndArray();
   }

   public <T> void writeArray(Collection<T> values, ThrowingConsumer<T, IOException> contentWriter) throws IOException {
      json.writeStartArray();
      for (T value : values) {
         contentWriter.accept(value);
      }
      json.writeEndArray();
   }
}
