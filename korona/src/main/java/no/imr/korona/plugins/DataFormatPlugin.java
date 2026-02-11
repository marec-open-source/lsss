package no.imr.korona.plugins;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BasePlugin;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Base class for plugins loading data.
 */
public abstract class DataFormatPlugin extends BasePlugin {
   private final String description;
   private final List<String> mainSuffixes;

   protected DataFormatPlugin(Name name, String description, List<String> mainSuffixes) {
      super(name);

      this.description = description;
      this.mainSuffixes = mainSuffixes;
   }

   public String getDescription() {
      return description;
   }

   public List<String> getMainSuffixes() {
      return mainSuffixes;
   }

   public abstract @Nullable SegmentHandle createSegmentHandle(Path file) throws IOException;

   public abstract List<SegmentHandle> createSegmentHandles(Set<Path> files, AsyncHandle asyncHandle) throws IOException;
}
