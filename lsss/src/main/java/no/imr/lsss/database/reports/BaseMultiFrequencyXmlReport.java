package no.imr.lsss.database.reports;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.logging.Level;

abstract class BaseMultiFrequencyXmlReport extends BaseMultiFrequencyReport {
   private @Nullable BufferedWriter writer;
   private @Nullable XMLStreamWriter xmlStreamWriter;

   BaseMultiFrequencyXmlReport(int type, ReportEngine reportEngine) {
      super(type, reportEngine);
   }

   private XMLStreamWriter xmlStreamWriter() {
      if (xmlStreamWriter == null) {
         throw new IllegalStateException("Writer is null");
      }
      return xmlStreamWriter;
   }

   @Override
   String getFileSuffix() {
      return ".xml";
   }

   @Override
   void open(PrintData.Pelagic aPrintData, Path aDirectory, ReportMode aMode, float aStartObservationDistance, float aStopObservationDistance) throws IOException {
      String postfix = GetPostfix.getPostfix(aPrintData);
      Path file;
      if (getReportEngine().getDistanceFileExtension()) {
         file = makeFile(aDirectory, aStartObservationDistance, aStopObservationDistance, postfix);
      } else {
         file = makeFile(aDirectory, postfix);
      }
      try {
         openXml(file);
         printHeaderXml(xmlStreamWriter(), aPrintData);
      } catch (XMLStreamException e) {
         throw new IOException("Error opening XML report", e);
      }
   }

   private void openXml(Path file) throws IOException, XMLStreamException {
      OutputStream outputStream = getReportEngine().newOutputStream(file);
      writer = new BufferedWriter(new OutputStreamWriter(outputStream, Utils.UTF_8));
      IndentingXMLStreamWriter indentingXMLEventWriter = new IndentingXMLStreamWriter(XMLOutputFactory.newFactory().createXMLStreamWriter(writer));
      indentingXMLEventWriter.setIndentStep("   ");
      xmlStreamWriter = indentingXMLEventWriter;
   }

   abstract void printHeaderXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData) throws XMLStreamException;

   @Override
   void printMetadata(PrintData.Pelagic aPrintData) {
      try {
         printMetadataXml(xmlStreamWriter(), aPrintData);
      } catch (XMLStreamException e) {
         Log.global.log(Level.WARNING, "Error writing XML report", e);
      }
   }

   void printMetadataXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData) throws XMLStreamException {
   }

   @Override
   void print(PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom) {
      try {
         printXml(xmlStreamWriter(), aPrintData, aMode, aPrintDataBottom);
      } catch (XMLStreamException e) {
         Log.global.log(Level.WARNING, "Error writing XML report", e);
      }
   }

   abstract void printXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom) throws XMLStreamException;

   @Override
   void finaliseReport(PrintData.Pelagic aPrintData) {
      XMLStreamWriter xmlStreamWriter = xmlStreamWriter();
      try {
         finaliseReportXml(xmlStreamWriter, aPrintData);
         xmlStreamWriter.writeCharacters("\n"); // com.sun.xml.txw2.output.IndentingXMLStreamWriter does not write final new line
      } catch (XMLStreamException e) {
         Log.global.log(Level.WARNING, "Error writing XML report tail", e);
      }
   }

   abstract void finaliseReportXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData) throws XMLStreamException;

   @Override
   void close() {
      if (xmlStreamWriter != null && writer != null) {
         try {
            xmlStreamWriter.close(); // Does not close the underlying output stream
            writer.close();
         } catch (IOException | XMLStreamException e) {
            Log.global.log(Level.WARNING, "Error closing XML report", e);
         }
         xmlStreamWriter = null;
         writer = null;
      }
   }

   void writeAttribute(String name, float value) throws XMLStreamException {
      xmlStreamWriter().writeAttribute(name, Utils.toString(value));
   }

   void writeAttribute(String name, int value) throws XMLStreamException {
      xmlStreamWriter().writeAttribute(name, Integer.toString(value));
   }

   void writeSimpleElement(String name, float value) throws XMLStreamException {
      writeSimpleElement(name, Utils.toString(value));
   }

   void writeSimpleElement(String name, int value) throws XMLStreamException {
      writeSimpleElement(name, Integer.toString(value));
   }

   void writeSimpleElement(String name, String value) throws XMLStreamException {
      XMLStreamWriter xmlStreamWriter = xmlStreamWriter();
      xmlStreamWriter.writeStartElement(name);
      xmlStreamWriter.writeCharacters(value);
      xmlStreamWriter.writeEndElement();
   }

   void writeEmptyElementWithAttribute(String elementName, String attributeName, String attributeValue) throws XMLStreamException {
      XMLStreamWriter xmlStreamWriter = xmlStreamWriter();
      xmlStreamWriter.writeEmptyElement(elementName);
      xmlStreamWriter.writeAttribute(attributeName, attributeValue);
   }
}
