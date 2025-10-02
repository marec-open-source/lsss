package no.imr.lsss.framework.export;

import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.misc.ReferenceDirectory;
import no.imr.tools.parameter.misc.ReferenceDirectoryCollection;
import no.imr.tools.parameter.misc.ReferenceDirectoryManager;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

public final class ExportSettings {
   final ReferenceDirectory exportReferenceDirectory = new ReferenceDirectory(new Name("ExportDir"));
   final ReferenceDirectoryManager referenceDirectoryManager = new ReferenceDirectoryManager()
         .add(new ReferenceDirectoryCollection(new Name("Export"), exportReferenceDirectory));
   String fileNamePrefix = "";
   @Nullable Element xml;

   public ExportSettings() {
   }
}
