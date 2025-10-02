package no.imr.korona.util.realtime.app;

import no.imr.korona.Korona;
import no.imr.korona.config.gui.ContextVisibility;
import no.imr.korona.util.CfsManager;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.Element;

import javax.swing.JFileChooser;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ProcessorConfig implements ParameterContainer {
   public final FileParameter cfsFile = new CfsParameter();

   public final FileParameter sourceDirectory = new FileParameter(
         new Name("SourceDirectory", "Source directory"),
         null, FileParameter.Mode.DIRECTORY);

   public final FileParameter destinationDirectory = new FileParameter(
         new Name("DestinationDirectory", "Destination directory"),
         null, FileParameter.Mode.DIRECTORY);

   final BooleanParameter onlyRaw = new BooleanParameter(
         new Name("OnlyRaw", "Read only the raw files"),
         false,
         "Read only the raw files in the input directory");

   private final CfsManager cfsManager;

   public ProcessorConfig(Korona korona) {
      cfsManager = new CfsManager(korona, cfsFile, null, ContextVisibility.HIDE);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            cfsFile,
            sourceDirectory,
            destinationDirectory,
            onlyRaw
      );
   }

   public CfsManager getCfsManager() {
      return cfsManager;
   }

   public void load(Path file) throws IOException {
      Document document = XmlUtils.readDocumentIfExists(file);
      if (document == null) {
         return;
      }
      Element xml = document.getRootElement();
      new ParameterCollection(this).fromXml(xml);
   }

   public void save(Path file) throws IOException {
      Element xml = new ParameterCollection(this).toXml();
      XmlUtils.writeDocument(xml, file);
   }

   private final class CfsParameter extends FileParameter {
      private CfsParameter() {
         super(new Name("ConfigFileSettings", "Config file settings"), null, Mode.FILE);
      }

      @Override
      public void customizeFileChooser(JFileChooser fileChooser) {
         fileChooser.setDialogTitle("Select config file settings");
      }

      @Override
      public FileParameter.Editor getEditor() {
         return cfsManager.createCfsEditor();
      }
   }
}
