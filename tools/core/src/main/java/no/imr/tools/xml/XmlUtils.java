package no.imr.tools.xml;

import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.Configurable;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.util.ByteBufferBackedInputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * XML utility functions.
 */
public final class XmlUtils {
   public static final String VERSION = "version";

   private static final ThreadLocal<SAXReader> THREAD_LOCAL_SAX_READER = ThreadLocal.withInitial(SAXReader::createDefault);

   private XmlUtils() {
   }

   public static boolean equalContent(Node nodeA, Node nodeB) {
      return Arrays.equals(toCompactBytes(nodeA), toCompactBytes(nodeB));
   }

   public static boolean equalContent(Document document, Path file) {
      try {
         byte[] fileBytes = Files.readAllBytes(file);
         byte[] documentBytes = toFileBytes(document, FileUtils.isGzip(file), fileBytes.length);
         return Arrays.equals(documentBytes, fileBytes);
      } catch (IOException _) {
         return false;
      }
   }

   public static boolean equalContent(Element element, Path file) {
      return equalContent(toDocument(element), file);
   }

   public static void writeDocument(Document document, Path file) throws IOException {
      byte[] bytes = toFileBytes(document, FileUtils.isGzip(file), 128);
      FileUtils.replaceFileSafely(file, bytes);
   }

   public static void writeDocument(Element element, Path file) throws IOException {
      writeDocument(toDocument(element), file);
   }

   public static Document toDocument(Element element) {
      Document document = element.getDocument();
      if (document != null && document.getRootElement() == element) {
         return document;
      }
      return DocumentHelper.createDocument(element);
   }

   public static @Nullable Document readDocumentIfExists(Path file) throws IOException {
      try {
         return readDocument(file);
      } catch (IOException e) {
         Throwable cause = e.getCause();
         if (cause != null && FileUtils.notExists(cause, file)) {
            return null;
         }
         throw e;
      }
   }

   public static Document readDocument(Path file) throws IOException {
      // NB: Cannot use FileReader since character encoding is inside XML file.
      try (InputStream fileInputStream = FileUtils.newBufferedInputStream(file);
           InputStream inputStream = FileUtils.isGzip(file) ? new GZIPInputStream(fileInputStream) : fileInputStream) {
         return readDocument(inputStream);
      } catch (IOException e) {
         throw new IOException("Error reading file " + file, e);
      }
   }

   public static Document readDocument(URL url) throws IOException {
      try (InputStream inputStream = url.openStream()) {
         return readDocument(inputStream);
      } catch (IOException e) {
         throw new IOException("Error reading url " + url, e);
      }
   }

   public static Document readDocument(String string) throws IOException {
      return readDocument(string.getBytes(Utils.UTF_8));
   }

   public static Document readDocument(byte[] bytes) throws IOException {
      try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
         return readDocument(in);
      }
   }

   public static Document readDocument(ByteBuffer byteBuffer) throws IOException {
      try (ByteBufferBackedInputStream in = new ByteBufferBackedInputStream(byteBuffer)) {
         return readDocument(in);
      }
   }

   public static Document readDocument(InputStream inputStream) throws IOException {
      try {
         SAXReader saxReader = THREAD_LOCAL_SAX_READER.get();
         return saxReader.read(inputStream);
      } catch (DocumentException e) {
         throw new XmlException(e);
      }
   }

   public static String readRootElementName(Path file) throws IOException {
      try (InputStream in = FileUtils.newBufferedInputStream(file)) {
         return readRootElementName(in);
      } catch (XMLStreamException | IOException e) {
         throw new IOException("Error reading file " + file, e);
      }
   }

   private static String readRootElementName(InputStream inputStream) throws XMLStreamException, IOException {
      XMLEventReader xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
      try {
         while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
               return xmlEvent.asStartElement().getName().getLocalPart();
            }
         }
         throw new XmlException("No root element");
      } finally {
         xmlEventReader.close();
      }
   }

   public static byte[] toFileBytes(Document document, boolean gzip, int initialSize) {
      try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(initialSize);
           OutputStream outputStream = gzip ? new GZIPOutputStream(byteArrayOutputStream) : byteArrayOutputStream) {
         document.normalize();
         XMLWriter xmlWriter = new XMLWriter(outputStream, prettyOutputFormat());
         xmlWriter.write(document);
         xmlWriter.close();
         return byteArrayOutputStream.toByteArray();
      } catch (IOException e) {
         // Should not happen since writing to memory only;
         throw new ShouldNotHappenException(e);
      }
   }

   private static OutputFormat prettyOutputFormat() {
      return new OutputFormat("   ", true);
   }

   public static byte[] toCompactBytes(Node node) {
      return toBytes(node, OutputFormat.createCompactFormat());
   }

   public static byte[] toDefaultBytes(Node node) {
      return toBytes(node, null);
   }

   private static byte[] toBytes(Node node, @Nullable OutputFormat outputFormat) {
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
         XMLWriter xmlWriter = outputFormat != null ? new XMLWriter(out, outputFormat) : new XMLWriter(out);
         xmlWriter.write(node);
         xmlWriter.close();
         return out.toByteArray();
      } catch (IOException e) {
         // Should not happen since writing to memory only;
         throw new ShouldNotHappenException(e);
      }
   }

   public static String toCompactString(Node node) {
      return toString(node, OutputFormat.createCompactFormat());
   }

   public static String toDefaultString(Node node) {
      return toString(node, null);
   }

   public static String toPrettyString(Node node) {
      return toString(node, prettyOutputFormat());
   }

   private static String toString(Node node, @Nullable OutputFormat outputFormat) {
      if (node instanceof Document document) {
         document.normalize();
      }
      try (StringWriter stringWriter = new StringWriter()) {
         XMLWriter xmlWriter = outputFormat != null ? new XMLWriter(stringWriter, outputFormat) : new XMLWriter(stringWriter);
         xmlWriter.write(node);
         xmlWriter.close();
         return stringWriter.getBuffer().toString();
      } catch (IOException e) {
         // Should not happen since writing to memory only;
         throw new ShouldNotHappenException(e);
      }
   }

   public static void removeBlankMixedContentText(Element element) {
      if (element.isTextOnly()) {
         return;
      }
      for (int i = element.nodeCount() - 1; i >= 0; i--) {
         Node node = element.node(i);
         if (node.getNodeType() == Node.TEXT_NODE && node.getText().isBlank()) {
            node.detach();
         }
      }
      element.elements().forEach(XmlUtils::removeBlankMixedContentText);
   }

   public static String getVersion(Element element) {
      String version = element.attributeValue(VERSION);
      if (version == null) {
         return "0";
      }
      return version;
   }

   public static String getPath(Element element) {
      StringBuilder stringBuilder = new StringBuilder();
      buildPath(element, stringBuilder);
      return stringBuilder.toString();
   }

   private static void buildPath(Element element, StringBuilder stringBuilder) {
      Element parent = element.getParent();
      if (parent == null) {
         return;
      }
      buildPath(parent, stringBuilder);
      stringBuilder.append('/').append(element.getName());
      Attribute attribute = element.attribute(Configurable.XML_NAME);
      if (attribute != null) {
         stringBuilder.append("[@").append(attribute.getName()).append('=').append(attribute.getValue()).append(']');
      }
   }

   public static Optional<Element> getFirstWithAttribute(Collection<Element> elements, String attributeName, String attributeValue) {
      for (Element element : elements) {
         if (attributeValue.equals(element.attributeValue(attributeName))) {
            return Optional.of(element);
         }
      }
      return Optional.empty();
   }
}
