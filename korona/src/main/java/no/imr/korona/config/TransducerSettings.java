package no.imr.korona.config;

import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * Holds paths to XML files for settings related to transducers.
 */
public final class TransducerSettings extends BaseSettings {
   public final FileParameter transducerParameterFile = new FileParameter(
         new Name("TransducerParameterFile", "Transducer parameter file"),
         null, FileParameter.Mode.FILE,
         "Transducer parameter file");

   private TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.ALL);

   public static final String XML_TRANSDUCER = "Transducers";

   public TransducerSettings() {
      super(new Name(XML_TRANSDUCER));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            transducerParameterFile
      );
   }

   @Override
   public void fromXml(Element element) {
      super.fromXml(element);
      Path file = transducerParameterFile.getFile();
      TransducerParameterManager manager = null;
      if (file != null) {
         try {
            Document document = XmlUtils.readDocument(file);
            manager = new TransducerParameterManager(TransducerParameters.ParameterType.ALL, document);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading " + file, e);
         }
      }
      if (manager == null) {
         manager = new TransducerParameterManager(TransducerParameters.ParameterType.ALL);
      }
      transducerParameterManager = manager;
      transducerParameterManager.getChangeManager().addListener(getChangeManager());
   }

   public TransducerParameterManager getTransducerParameterManager() {
      return transducerParameterManager;
   }

   public void writeToTransducerFile() {
      Path file = transducerParameterFile.getFile();
      if (file != null) {
         try {
            XmlUtils.writeDocument(transducerParameterManager.toXml(), file);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error saving " + file, e);
         }
      }
   }
}
