package no.imr.korona.computation.plankton;

import no.imr.tools.Utils;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.RangeMap;
import no.imr.tools.range.RangeUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the size distributions for plankton inversion in an XML file.
 */
public final class PlanktonFile {
   public static final String PLANKTON_XML_FILE = "Plankton.xml";

   public static final String XML_PLANKTON = "plankton";
   public static final String XML_MODEL = "model";
   private static final String XML_NAME = "name";
   private static final String XML_SIZE_FACTOR = "sizeFactor";

   private final Map<String, List<PlanktonRectangle>> nameToPlanktonRectangles = new TreeMap<>();

   private double sizeFactor;

   public PlanktonFile() {
   }

   public PlanktonFile(Path file) throws IOException {
      this(XmlUtils.readDocument(file));
   }

   public PlanktonFile(Document document) throws PlanktonFileException {
      Element root = document.getRootElement();

      sizeFactor = Utils.parseDouble(root.attributeValue(XML_SIZE_FACTOR), 1);
      for (Element element : root.elements(XML_MODEL)) {
         String name = element.attributeValue(XML_NAME);
         PlanktonRectangle planktonRectangle = new PlanktonRectangle(element, sizeFactor);
         addEntry(name, planktonRectangle);
      }
   }

   public void addEntry(String name, PlanktonRectangle planktonRectangle) {
      nameToPlanktonRectangles.computeIfAbsent(name, _ -> new ArrayList<>()).add(planktonRectangle);
   }

   public Element toXml() {
      Element element = DocumentHelper.createElement(XML_PLANKTON)
            .addAttribute(XML_SIZE_FACTOR, Double.toString(sizeFactor));
      for (Map.Entry<String, List<PlanktonRectangle>> entry : nameToPlanktonRectangles.entrySet()) {
         for (PlanktonRectangle planktonRectangle : entry.getValue()) {
            if (!entry.getKey().isEmpty() && planktonRectangle.getSizeHistogram().getDividers().length > 0) {
               Element modelElement = element.addElement(XML_MODEL)
                     .addAttribute(XML_NAME, entry.getKey());
               planktonRectangle.toXml(modelElement, sizeFactor);
            }
         }
      }
      return element;
   }

   public double getSizeFactor() {
      return sizeFactor;
   }

   public void setSizeFactor(double sizeFactor) {
      this.sizeFactor = sizeFactor;
   }

   public RangeMap<Float, PlanktonRectangle> getDepthMap(String name, long timeInMillis) {
      List<PlanktonRectangle> planktonRectangles = nameToPlanktonRectangles.get(name);
      if (planktonRectangles != null) {
         RangeMap<Float, PlanktonRectangle> rangeMap = new ArrayRangeMap<>();
         for (PlanktonRectangle planktonRectangle : planktonRectangles) {
            if (planktonRectangle.isUse() && planktonRectangle.getMillisRange().contains(timeInMillis)) {
               rangeMap.put(planktonRectangle.getDepthRange(), planktonRectangle);
            }
         }
         return rangeMap;
      } else {
         return RangeUtils.emptyRangeMap();
      }
   }

   public Map<String, List<PlanktonRectangle>> getPlanktonRectangles() {
      return nameToPlanktonRectangles;
   }
}
