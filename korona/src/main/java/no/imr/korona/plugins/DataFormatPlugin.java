package no.imr.korona.plugins;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BasePlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Base class for plugins loading data.
 */
public abstract class DataFormatPlugin extends BasePlugin {
   protected DataFormatPlugin(Name name) {
      super(name);
   }

   public abstract String getDescription();

   public abstract List<String> getMainSuffixes();

   public abstract List<String> getCanOpenSuffixes();

   public boolean canCreateSegmentHandle(Path file) {
      String path = file.toString();
      for (String suffix : getCanOpenSuffixes()) {
         if (path.endsWith(suffix)) {
            return true;
         }
      }
      return false;
   }

   public abstract SegmentHandle createSegmentHandle(Path file) throws IOException;

   public abstract List<SegmentHandle> createSegmentHandles(Set<Path> files, AsyncHandle asyncHandle) throws IOException;
}
