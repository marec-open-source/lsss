package no.imr.korona.computation.offset;

import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data structure for transducer parameter configuration.
 * I/O to xml file for persistence of configuration.
 * Keeps a sorted List of TransducerParameters objects.
 */
public final class TransducerParameterManager extends Configurable {
   public static final String XML_CORRECTIONS = "corrections";
   public static final String XML_TRANSDUCER = "transducer";
   public static final String XML_TYPE = "type";

   private final List<TransducerParameters> transducerParametersList = new ArrayList<>();
   private final TransducerParameters.ParameterType parameterType;
   private final ChangeManager changeManager = new ChangeManager();

   public TransducerParameterManager(TransducerParameters.ParameterType parameterType) {
      super(new Name(XML_CORRECTIONS));

      this.parameterType = parameterType;
   }

   public TransducerParameterManager(TransducerParameters.ParameterType parameterType, Document document) {
      this(parameterType);

      fromXml(document.getRootElement());
   }

   public TransducerParameters.ParameterType getParameterType() {
      return parameterType;
   }

   public void sortAndNotify() {
      transducerParametersList.sort(null);
      changeManager.notifyListeners();
   }

   public Optional<TransducerParameters> getTransducerOffsetPar(int kHz) {
      int i = binarySearchForKHz(kHz);
      return i >= 0 ? Optional.of(transducerParametersList.get(i)) : Optional.empty();
   }

   private int binarySearchForKHz(int kHz) {
      return Utils.binarySearchForInt(transducerParametersList, kHz, t -> t.frequency.getIntValue());
   }

   public Optional<Float> getBlindZone(int kHz) {
      int i = binarySearchForKHz(kHz);
      if (i >= 0) {
         return Optional.of(transducerParametersList.get(i).blindZone.getFloatValue());
      }
      i = -(i + 1); // conversion to insertion point
      if (i == 0 || i == transducerParametersList.size()) {
         return Optional.empty();
      }
      TransducerParameters p1 = transducerParametersList.get(i - 1);
      TransducerParameters p2 = transducerParametersList.get(i);
      float blindZone = Math.max(p1.blindZone.getFloatValue(), p2.blindZone.getFloatValue());
      return Optional.of(blindZone);
   }

   public Optional<Float> getRange(int kHz) {
      int i = binarySearchForKHz(kHz);
      if (i >= 0) {
         return Optional.of(transducerParametersList.get(i).range.getFloatValue());
      }
      i = -(i + 1); // conversion to insertion point
      if (i == 0 || i == transducerParametersList.size()) {
         return Optional.empty();
      }
      TransducerParameters p1 = transducerParametersList.get(i - 1);
      TransducerParameters p2 = transducerParametersList.get(i);
      float x1 = p1.frequency.getIntValue();
      float y1 = p1.range.getFloatValue();
      float x2 = p2.frequency.getIntValue();
      float y2 = p2.range.getFloatValue();
      float range = y1 + (kHz - x1) * (y2 - y1) / (x2 - x1);
      return Optional.of(range);
   }

   public List<Integer> getKHzs() {
      List<Integer> kHzs = new ArrayList<>();
      for (TransducerParameters trans : transducerParametersList) {
         kHzs.add(trans.getKHz());
      }
      return kHzs;
   }

   @Override
   public Element toXml() {
      Element element = DocumentHelper.createElement(XML_CORRECTIONS)
            .addAttribute(XML_TYPE, parameterType.toString());
      for (TransducerParameters entry : transducerParametersList) {
         element.add(entry.toXml());
      }
      return element;
   }

   @Override
   public void fromXml(Element element) {
      transducerParametersList.clear();
      for (Element transducerElement : element.elements(XML_TRANSDUCER)) {
         transducerParametersList.add(new TransducerParameters(parameterType, transducerElement));
      }
      sortAndNotify();
   }

   public List<TransducerParameters> getTransducerParametersList() {
      return transducerParametersList;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }
}
