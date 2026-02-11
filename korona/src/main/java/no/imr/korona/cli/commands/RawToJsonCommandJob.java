package no.imr.korona.cli.commands;

import no.imr.korona.Korona;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.cli.CliException;
import no.imr.korona.cli.CliUtils;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.formats.ek60.io.BaseDatagramReader;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.JsonUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class RawToJsonCommandJob extends CliCommandJob {
   private final List<Path> files;
   private final Korona korona = new Korona();

   RawToJsonCommandJob(List<Path> files) {
      this.files = files;
      if (files.isEmpty()) {
         throw new CliException("Please specify one or more files or directories");
      }
   }

   @Override
   public void run(InputStream in, PrintStream out) throws IOException {
      for (Path file : files) {
         if (Files.isDirectory(file)) {
            printDir(out, file);
         } else {
            printFile(out, file);
         }
      }
   }

   private void printDir(PrintStream out, Path dir) throws IOException {
      for (Path file : FileUtils.listFiles(dir)) {
         out.println(CliUtils.FILE_SEPARATOR);
         printFile(out, file);
      }
   }

   private void printFile(PrintStream out, Path file) {
      JsonMapper jsonMapper = JsonUtils.JSON_MAPPER.rebuild()
            .addModule(byteArrayAsArrayModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();
      try (BaseDatagramReader datagramReader = new FileDatagramReader(file, korona.getDatagramTypeManager());
           JsonGenerator json = jsonMapper.createGenerator(out)) {

         json.writeStartArray();
         while (true) {
            BaseDatagram datagram = datagramReader.nextDatagram();
            if (datagram == null) {
               break;
            }
            json.writePOJO(datagram);
         }
         json.writeEndArray();
      } catch (IOException e) {
         throw new CliException("Error reading " + file, e);
      }
   }

   private static SimpleModule byteArrayAsArrayModule() {
      SimpleModule module = new SimpleModule();
      module.addSerializer(new ByteArrayAsArraySerializer());
      return module;
   }

   private static final class ByteArrayAsArraySerializer extends StdSerializer<byte[]> {
      private ByteArrayAsArraySerializer() {
         super(byte[].class);
      }

      @Override
      public void serialize(byte[] value, JsonGenerator json, SerializationContext provider) {
         json.writeStartArray();
         for (byte b : value) {
            json.writeNumber(b & 0xff);
         }
         json.writeEndArray();
      }
   }
}
