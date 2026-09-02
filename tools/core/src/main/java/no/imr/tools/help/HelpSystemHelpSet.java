package no.imr.tools.help;

import no.imr.tools.ResourceUtils;
import no.imr.tools.help.pojo.SimplePojoHelpSet;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.JsonUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * A help set with knowledge about its parent.
 */
public final class HelpSystemHelpSet {
   public static final HelpSystemHelpSet EMPTY = new HelpSystemHelpSet();

   private final String id;
   private final @Nullable String helpDir;
   private final @Nullable ClassLoader classLoader;
   private volatile @Nullable Set<String> validIds;
   private final HelpID topHelpID = createHelpID("");
   private @Nullable HelpSystem helpSystem;

   private HelpSystemHelpSet() {
      id = "";
      helpDir = null;
      classLoader = null;
   }

   public HelpSystemHelpSet(String helpDir, String id) {
      this.id = id;
      this.helpDir = helpDir;
      classLoader = null;
   }

   public HelpSystemHelpSet(String helpDir, String id, ClassLoader classLoader) {
      this.id = id;
      this.helpDir = helpDir;
      this.classLoader = classLoader;
   }

   @Override
   public String toString() {
      return id;
   }

   public String getId() {
      return id;
   }

   public @Nullable String getHelpDir() {
      return helpDir;
   }

   public @Nullable ClassLoader getClassLoader() {
      return classLoader;
   }

   Set<String> getValidIds() {
      Set<String> ids = validIds;
      if (ids == null) {
         ids = loadValidIds();
         validIds = ids;
      }
      return ids;
   }

   private Set<String> loadValidIds() {
      if (helpDir == null) {
         return Set.of();
      }
      String helpSetResource = helpDir + "/build/helpSet.json";
      try {
         SimplePojoHelpSet simplePojoHelpSet = JsonUtils.readValue(ResourceUtils.getUrl(helpSetResource), SimplePojoHelpSet.class);
         Set<String> ids = HashSet.newHashSet(simplePojoHelpSet.pageIds.size() + simplePojoHelpSet.aliases.size());
         ids.addAll(simplePojoHelpSet.pageIds);
         ids.addAll(simplePojoHelpSet.aliases.keySet());
         return Set.copyOf(ids);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error loading " + helpSetResource, e);
         return Set.of();
      }
   }

   public HelpID createHelpID(String helpID) {
      return new HelpID(helpID, this);
   }

   public HelpID getTopHelpID() {
      return topHelpID;
   }

   @Nullable HelpSystem getHelpSystem() {
      return helpSystem;
   }

   void setHelpSystem(HelpSystem helpSystem) {
      if (this.helpSystem != null) {
         // Assume the first help system is the main one.
         return;
      }
      this.helpSystem = helpSystem;
   }
}
