package no.imr.tools.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import no.imr.tools.io.FileUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Path;

public final class JaxbUtils {
   private JaxbUtils() {
   }

   public static <T> T read(Class<T> clazz, Path file) throws IOException {
      try (InputStream in = FileUtils.newBufferedInputStream(file)) {
         return clazz.cast(createUnmarshaller(clazz).unmarshal(in));
      } catch (JAXBException | RuntimeException e) {
         throw new IOException(e);
      }
   }

   public static <T> T read(Class<T> clazz, String content) throws IOException {
      try {
         return clazz.cast(createUnmarshaller(clazz).unmarshal(Reader.of(content)));
      } catch (JAXBException | RuntimeException e) {
         throw new IOException(e);
      }
   }

   public static Object readIgnoringNamespace(Class<?> clazz, InputStream in) throws IOException {
      try {
         XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
         xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
         XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new StreamSource(in));
         try {
            return createUnmarshaller(clazz).unmarshal(xmlStreamReader);
         } finally {
            xmlStreamReader.close(); // This method does not close the underlying input source.
         }
      } catch (JAXBException | RuntimeException | XMLStreamException e) {
         throw new IOException(e);
      }
   }

   public static void write(Object object, Path file) throws IOException {
      FileUtils.replaceFileSafely(file, toBytes(object));
   }

   public static void write(Object object, PrintStream out) throws IOException {
      try {
         createMarshaller(object).marshal(object, out);
      } catch (JAXBException e) {
         throw new IOException(e);
      }
   }

   public static byte[] toBytes(Object object) throws IOException {
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
         createMarshaller(object).marshal(object, out);
         return out.toByteArray();
      } catch (JAXBException e) {
         throw new IOException(e);
      }
   }

   public static String toString(Object object) throws IOException {
      try (StringWriter out = new StringWriter()) {
         createMarshaller(object).marshal(object, out);
         return out.toString();
      } catch (JAXBException e) {
         throw new IOException(e);
      }
   }

   private static Marshaller createMarshaller(Object object) throws JAXBException {
      Marshaller marshaller = JAXBContext.newInstance(object.getClass()).createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      return marshaller;
   }

   private static Unmarshaller createUnmarshaller(Class<?> clazz) throws JAXBException {
      return JAXBContext.newInstance(clazz).createUnmarshaller();
   }
}
