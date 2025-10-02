package no.imr.lsss.framework.config;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Name;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Common functionality for the main configuration units.
 */
public abstract class MainConfigurationUnit extends ConfigurationUnit {
   private final Path file;

   protected MainConfigurationUnit(BaseSystemFeaturePlugin plugin, Name name, String description, String defaultConfigFileName) {
      super(plugin, name, description);

      file = getConfigFile(defaultConfigFileName);
   }

   public static Path getConfigFile(String fileName) {
      return LSSS.getApplicationDataDir().resolve("config").resolve(fileName);
   }

   public void loadDefault() {
      try {
         Document document = XmlUtils.readDocumentIfExists(file);
         if (document != null) {
            fromXml(document.getRootElement());
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error reading " + file, e);
      }
   }

   protected Element toDefaultXml() {
      return toXml();
   }

   public void saveDefault() {
      if (!getLSSS().getLsssConfig().isPrimaryLSSS) {
         return;
      }
      try {
         XmlUtils.writeDocument(toDefaultXml(), file);
      } catch (IOException e) {
         getLSSS().showError("Error saving " + file, e);
      }
   }

   public boolean isDefaultModified() {
      return !XmlUtils.equalContent(toDefaultXml(), file);
   }
}
