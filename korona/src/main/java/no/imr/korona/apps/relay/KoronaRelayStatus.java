package no.imr.korona.apps.relay;

import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * File for holding data from status.xml.
 * It holds a list of {@link KoronaRelayUpdate}s, which based on an input filename
 * knows the destination files to copy to.
 */
final class KoronaRelayStatus {
   private static final String XML_STATUS = "status";
   private static final String XML_FILE = "file";
   private static final String XML_NAME = "name";
   private static final String XML_PROCESSING = "processing";

   private final Path file;
   private final Set<Path> filesReadyForCopy = new LinkedHashSet<>();
   private final Set<Path> filesBeingProcessed = new LinkedHashSet<>();

   KoronaRelayStatus(Path file) throws IOException {
      this.file = file;

      Document doc = XmlUtils.readDocumentIfExists(file);
      if (doc != null) {
         Path dir = file.getParent();
         Element rootElement = doc.getRootElement();
         parseFiles(rootElement, dir, filesReadyForCopy);

         Element processingElement = rootElement.element(XML_PROCESSING);
         if (processingElement != null) {
            parseFiles(processingElement, dir, filesBeingProcessed);
         }
      }
   }

   private static void parseFiles(Element element, Path dir, Set<Path> files) {
      for (Element fileElement : element.elements(XML_FILE)) {
         files.add(dir.resolve(fileElement.attributeValue(XML_NAME)));
      }
   }

   List<KoronaRelayUpdate> getAllCompletedFiles() {
      return filesReadyForCopy.stream()
            .map(file -> new KoronaRelayUpdate(this, file))
            .toList();
   }

   Set<Path> getFilesReadyForCopy() {
      return filesReadyForCopy;
   }

   Set<Path> getFilesBeingProcessed() {
      return filesBeingProcessed;
   }

   private Element toXml() {
      Element rootElement = filesToXml(XML_STATUS, filesReadyForCopy);

      if (!filesBeingProcessed.isEmpty()) {
         rootElement.add(filesToXml(XML_PROCESSING, filesBeingProcessed));
      }

      return rootElement;
   }

   private static Element filesToXml(String elementName, Set<Path> files) {
      Element element = DocumentHelper.createElement(elementName);
      for (Path file : files) {
         element.addElement(XML_FILE)
               .addAttribute(XML_NAME, file.getFileName().toString());
      }
      return element;
   }

   void save() throws IOException {
      XmlUtils.writeDocument(toXml(), file);
   }
}
