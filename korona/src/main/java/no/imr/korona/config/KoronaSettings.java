package no.imr.korona.config;

import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.misc.ReferenceDirectory;
import no.imr.tools.parameter.misc.ReferenceDirectoryCollection;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Global settings common for KORONA, KoronaRelay and LSSS.
 */
public final class KoronaSettings extends Configurable {
   private final Path file;

   private final ReferenceDirectory koronaConfigDir = new ReferenceDirectory(new Name("KoronaConfigDir", "Korona config dir"));
   private final ReferenceDirectoryCollection referenceDirectoryCollection = new ReferenceDirectoryCollection(new Name("ReferenceDirectories"), koronaConfigDir);

   public KoronaSettings(Path file) {
      super(new Name("KoronaSettings"));

      this.file = file;

      load();
   }

   private void load() {
      try {
         Document document = XmlUtils.readDocumentIfExists(file);
         if (document != null) {
            fromXml(document.getRootElement());
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error reading KORONA settings " + file, e);
      }
   }

   public void save() {
      try {
         XmlUtils.writeDocument(toXml(), file);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error saving KORONA settings " + file, e);
      }
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return List.of(referenceDirectoryCollection);
   }

   public ReferenceDirectoryCollection getReferenceDirectoryCollection() {
      return referenceDirectoryCollection;
   }

   public ReferenceDirectory getKoronaConfigDir() {
      return koronaConfigDir;
   }
}
